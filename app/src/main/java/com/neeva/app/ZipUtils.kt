// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import okhttp3.internal.closeQuietly
import timber.log.Timber

object ZipUtils {
    /** Extracts a ZIP file that was provided via a content:// Uri. */
    @WorkerThread
    fun extract(context: Context, contentUri: Uri, outputDirectory: File): Boolean {
        var fileStream: InputStream? = null
        var zipStream: ZipInputStream? = null
        return try {
            fileStream = context.contentResolver.openInputStream(contentUri)
            zipStream = ZipInputStream(BufferedInputStream(fileStream))

            outputDirectory.mkdirs()

            var zipEntry: ZipEntry?
            while (zipStream.nextEntry.also { zipEntry = it } != null) {
                zipEntry?.let { entry -> extractZipEntry(entry, outputDirectory, zipStream) }
            }

            true
        } catch (throwable: Exception) {
            Timber.e(
                t = throwable,
                message = "Failed to unzip $contentUri"
            )
            false
        } finally {
            zipStream?.closeQuietly()
            fileStream?.closeQuietly()
        }
    }

    @WorkerThread
    private fun extractZipEntry(entry: ZipEntry, outputDirectory: File, zipStream: ZipInputStream) {
        var size: Int
        val buffer = ByteArray(1024)

        // Make sure that the directory exists before we try to write to it.
        val outputEntryFile = File(outputDirectory, entry.name)
        if (!outputEntryFile.canonicalPath.startsWith(outputDirectory.canonicalPath)) {
            throw SecurityException()
        }

        outputEntryFile.parentFile?.mkdirs()

        Timber.d("Unzipping: ${entry.name} to ${outputEntryFile.path}")
        FileOutputStream(outputEntryFile).use { outputStream ->
            BufferedOutputStream(outputStream, buffer.size).use { bufferedOutputStream ->
                while (zipStream.read(buffer, 0, buffer.size).also { size = it } != -1) {
                    bufferedOutputStream.write(buffer, 0, size)
                }
                bufferedOutputStream.flush()
            }
        }
    }

    @WorkerThread
    fun compress(file: File, outputZipFile: File): Boolean {
        var fileStream: FileOutputStream? = null
        var zipStream: ZipOutputStream? = null
        return try {
            fileStream = FileOutputStream(outputZipFile)
            zipStream = ZipOutputStream(fileStream)
            addFile("", file, zipStream)
            zipStream.flush()
            true
        } catch (exception: Exception) {
            Timber.e("Failed to zip $file", exception)
            false
        } finally {
            zipStream?.closeQuietly()
            fileStream?.closeQuietly()
        }
    }

    private fun addFile(internalZipPath: String, file: File, zipStream: ZipOutputStream) {
        if (file.isDirectory) {
            // Go through each file in the directory and zip it up.
            file.listFiles()?.forEach { descendant ->
                val prefix = "$internalZipPath/".takeIf { internalZipPath.isNotEmpty() } ?: ""
                val childPath = "$prefix${file.name}"

                addFile(
                    internalZipPath = childPath,
                    file = descendant,
                    zipStream = zipStream
                )
            }
        } else {
            // Read in the file and save it out to the compressed stream.
            zipStream.putNextEntry(ZipEntry(internalZipPath + "/" + file.name))

            val buffer = ByteArray(1024)
            var length: Int
            val inputStream = FileInputStream(file)
            while (inputStream.read(buffer).also { length = it } > 0) {
                zipStream.write(buffer, 0, length)
            }
        }
    }
}
