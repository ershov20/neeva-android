// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.common.util.Hex
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.use
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class Checksum(
    /** SHA-256 hash of the Bloom Filter file. */
    @Json(name = "sha") val sha: String,
    /** MD5 hash of the Bloom Filter file. */
    @Json(name = "md5") val md5: String,
    /** Hashed version code of the Bloom Filter hasher. */
    @Json(name = "hash") val hash: String,
    /** Hashed version code of the canonicalization algorithm. */
    @Json(name = "canon") val canon: String
) {
    companion object {
        /**
         * Hashed version of the Bloom Filter hash function.
         * Update when Bloom Filter hash function is updated.
         */
        private const val HASHER = "fe7351f"
        /**
         * Hashed version v3 of the canonicalization.
         * Update when URL canonicalization is updated.
         */
        private const val CANONICAL = "5aa27ef"

        fun loadChecksum(checksumFile: String): Checksum? {
            val moshi = Moshi.Builder().build()
            return try {
                val jsonAdapter = moshi.adapter(Checksum::class.java)
                jsonAdapter.fromJson(checksumFile)
            } catch (throwable: NullPointerException) {
                Timber.e(
                    t = throwable,
                    message = "Failed to get JSON adapter for Checksum "
                )
                null
            } catch (throwable: IllegalArgumentException) {
                Timber.e(
                    t = throwable,
                    message = "Failed to get JSON adapter for Checksum "
                )
                null
            } catch (throwable: IOException) {
                Timber.e(
                    t = throwable,
                    message = "Failed to load checksum file "
                )
                null
            }
        }

        fun verifyFile(fileUri: Uri, checksum: Checksum): Boolean {
            val fileSha =
                getChecksumFromFile(MessageDigest.getInstance("SHA-256"), fileUri)
            val fileMd5 = getChecksumFromFile(MessageDigest.getInstance("MD5"), fileUri)

            return checksum.sha == fileSha && checksum.md5 == fileMd5 &&
                checksum.hash == HASHER && checksum.canon == CANONICAL
        }

        // Calculate checksum hash for a file
        // ref: https://www.geeksforgeeks.org/how-to-generate-md5-checksum-for-files-in-java/
        fun getChecksumFromFile(digest: MessageDigest, fileUrl: Uri): String {
            try {
                fileUrl.toFile().inputStream().use {
                    val size = 1024
                    val byteArray = ByteArray(size)
                    var numBytesRead = it.read(byteArray, 0, size)
                    while (numBytesRead > -1) {
                        digest.update(byteArray, 0, numBytesRead)
                        numBytesRead = it.read(byteArray, 0, size)
                    }
                }

                val digestBytes = digest.digest()
                return Hex.bytesToStringLowercase(digestBytes)
            } catch (throwable: IOException) {
                Timber.e(
                    t = throwable,
                    message = "Failed to get checksum from file "
                )
                return ""
            }
        }
    }
}

/** Downloading pipeline: downloading and preparing the Bloom Filters. */
class BloomFilterDownloadWorker(
    val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        private const val DIRECTORY_BLOOM_FILTER = "bloom_filter"
        private const val MAX_RETRY_COUNT = 1
    }

    private val reddit = BloomFilterConfiguration.redditConfiguration

    override suspend fun doWork(): Result {
        // This runAttemptCount gets reset between periods for periodic work.
        if (runAttemptCount > MAX_RETRY_COUNT) { return Result.failure() }

        try {
            reddit.localUri = getFilterBinaryFile("reddit.bin").toUri()
        } catch (throwable: IOException) {
            Timber.e(
                t = throwable,
                message = "Failed to get filter file "
            )
            return Result.retry()
        } catch (throwable: IllegalArgumentException) {
            Timber.e(
                t = throwable,
                message = "Failed to get filter file Uri "
            )
            return Result.retry()
        }

        downloadFile()

        // Indicate whether the download work finished successfully with the Result
        if (reddit.localUri == Uri.EMPTY || !reddit.localUri.toFile().exists()) {
            return Result.retry()
        }
        return Result.success()
    }

    private suspend fun downloadFile() = withContext(Dispatchers.IO) {
        // Check url validity
        assert(reddit.checksumUrl.toString().endsWith(".json"))
        assert(reddit.filterUrl.toString().endsWith(".bin"))
        assert(reddit.localUri.scheme == "file")

        BloomFilterDownloader(reddit.checksumUrl, reddit.filterUrl, reddit.localUri)
            .download()
    }

    private suspend fun getFilterBinaryFile(name: String): File {
        val file = File(appContext.cacheDir, DIRECTORY_BLOOM_FILTER)
        if (!file.exists()) file.mkdir()
        return File(file, name)
    }
}

class BloomFilterDownloader(
    val checksumUrl: URL,
    val filterUrl: URL,
    val localUri: Uri
) {
    val okHttpClient = OkHttpClient().newBuilder().build()

    suspend fun download() {
        val bloomFilterFile = Uri.parse(localUri.toString()).toFile()

        // Download and load checksum file
        val checksumRequest = Request.Builder().url(checksumUrl).build()
        val checksum = requestDownload(okHttpClient, checksumRequest)?.use { response ->
            response.body?.string()?.let { body ->
                Checksum.loadChecksum(body)
            } ?: return
        } ?: return

        // Check if the existing local filter file matches new checksum
        // If match, don't update the existing local filter file
        if (bloomFilterFile.exists() && Checksum.verifyFile(bloomFilterFile.toUri(), checksum)) {
            return
        }

        // If not match, download new binary file
        val filterRequest = Request.Builder().url(filterUrl).build()
        requestDownload(okHttpClient, filterRequest)?.use { response ->
            try {
                val tempFile = File.createTempFile("reddit", ".tmp")
                response.body?.source()?.inputStream()?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return

                // If the filter file matches checksum, rename temp file
                if (Checksum.verifyFile(Uri.fromFile(tempFile), checksum)) {
                    if (bloomFilterFile.exists()) bloomFilterFile.delete()
                    tempFile.renameTo(bloomFilterFile)
                }
            } catch (throwable: IOException) {
                Timber.e(
                    t = throwable,
                    message = "Failed to download filter file "
                )
                return
            }
        }
    }

    private fun requestDownload(okHttpClient: OkHttpClient, request: Request): Response? {
        return try {
            okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            Timber.e("Failed to download checksum or filter file ", e)
            null
        } catch (e: IllegalStateException) {
            Timber.e("Failed to download checksum or filter file ", e)
            null
        }
    }
}
