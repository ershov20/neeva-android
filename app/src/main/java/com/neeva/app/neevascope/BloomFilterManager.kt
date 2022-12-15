// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neeva.app.Dispatchers
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import java.io.IOException
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val settingsDataModel: SettingsDataModel
) {
    companion object {
        private const val TAG = "BloomFilterDownloadWork"
    }

    val filterDownloadEnabled =
        settingsDataModel.getSettingsToggleValue(SettingsToggle.BLOOM_FILTER_DOWNLOAD)

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
        if (filterDownloadEnabled) {
            enqueueBloomFilterDownloadWorkRequest()
        }

        if (reddit.localUri == Uri.EMPTY || !reddit.localUri.toFile().exists()) return

        try {
            bloomFilter.loadFilter(reddit.localUri)
        } catch (throwable: IOException) {
            Timber.e(
                t = throwable,
                message = "Failed to load filter "
            )
        }
    }

    private fun enqueueBloomFilterDownloadWorkRequest() {
        val workRequestId = SharedPrefFolder.App.DownloadRequestId.get(sharedPreferencesModel)
        if (workRequestId == "") {
            workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                createWorkRequest()
            )
        } else {
            val workInfo =
                workManager.getWorkInfoById(UUID.fromString(workRequestId)).get()

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
                    createWorkRequest()
                )
            }
        }
    }

    private fun createWorkRequest(): PeriodicWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<BloomFilterDownloadWorker>(24, TimeUnit.HOURS)
                .addTag(TAG)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        SharedPrefFolder.App.DownloadRequestId.set(
            sharedPreferencesModel = sharedPreferencesModel,
            value = workRequest.id.toString()
        )

        return workRequest
    }
}
