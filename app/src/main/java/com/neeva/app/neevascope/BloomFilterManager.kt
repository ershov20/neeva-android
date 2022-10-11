// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neeva.app.Dispatchers
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Used to save info about bloom filter resource configuration.
 */
data class BloomFilterConfiguration(
    val identifier: String,
    val filterUrl: URL,
    val checksumUrl: URL,
    var localUri: Uri
) {
    companion object {
        val redditConfiguration: BloomFilterConfiguration = BloomFilterConfiguration(
            identifier = "reddit",
            filterUrl = URL("https://s.neeva.co/web/neevascope/v1/reddit.bin"),
            checksumUrl = URL("https://s.neeva.co/web/neevascope/v1/reddit_latest.json"),
            localUri = Uri.EMPTY
        )
    }
}

class BloomFilterManager(
    val appContext: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val sharedPreferencesModel: SharedPreferencesModel
) {
    companion object {
        private const val TAG = "BloomFilterManager"
        private const val DIRECTORY_BLOOM_FILTER = "bloom_filter"
    }

    val reddit = BloomFilterConfiguration.redditConfiguration
    val bloomFilter = BloomFilter()
    val workManager = WorkManager.getInstance(appContext)
    val loadAttempted = AtomicBoolean(false)

    fun contains(key: String): Boolean? {
        if (bloomFilter.filter == null) {
            if (loadAttempted.compareAndSet(false, true)) {
                coroutineScope.launch(dispatchers.io) {
                    load()
                }
            }
            return null
        }

        return bloomFilter.mayContain(key)
    }

    /**
     * Loading pipeline: enqueuing Worker to download the file, loading the Bloom Filter files and
     * running queries against the loaded Bloom Filters.
     * */
    private suspend fun load() {
        try {
            reddit.localUri = getFilterBinaryFile("reddit.bin").toUri()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to get filter file ", e)
            return
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to get filter file Uri ", e)
            return
        }

        val workerRequestId = SharedPrefFolder.App.DownloadRequestId.get(sharedPreferencesModel)
        if (workerRequestId == "") {
            workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                createWorkerRequest()
            )
        } else {
            val workInfo =
                workManager.getWorkInfoById(UUID.fromString(workerRequestId)).get()

            /**
             * isFinished() returns true if the State of WorkRequest is considered finished.
             * @return True for SUCCEEDED, FAILED, and CANCELLED states
             * Note that PeriodicWorkRequests will never enter this state (they will simply go back
             * to ENQUEUED and be eligible to run again)
             */
            if (workInfo == null || workInfo.state.isFinished) {
                workManager.enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    createWorkerRequest()
                )
            }
        }

        if (!reddit.localUri.toFile().exists()) return

        try {
            bloomFilter.loadFilter(reddit.localUri)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load filter ", e)
        }
    }

    @SuppressLint("IdleBatteryChargingConstraints")
    private fun createWorkerRequest(): PeriodicWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiresStorageNotLow(true)
            .build()

        val workerRequest =
            PeriodicWorkRequestBuilder<BloomFilterDownloadWorker>(Duration.ofHours(24L))
                .setConstraints(constraints)
                .build()

        SharedPrefFolder.App.DownloadRequestId.set(
            sharedPreferencesModel = sharedPreferencesModel,
            value = workerRequest.id.toString()
        )

        return workerRequest
    }

    private suspend fun getFilterBinaryFile(name: String): File {
        val file = File(appContext.cacheDir, DIRECTORY_BLOOM_FILTER)
        if (!file.exists()) file.mkdir()
        return File(file, name)
    }
}
