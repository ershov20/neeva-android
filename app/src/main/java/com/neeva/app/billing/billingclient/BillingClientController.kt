// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing.billingclient

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.neeva.app.Dispatchers
import com.neeva.app.InitializeGooglePlaySubscriptionMutation
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Fetches and posts information about Google Play Store subscriptions.
 * It is currently sand-boxed by [SettingsToggle.DEBUG_ENABLE_BILLING].
 */
class BillingClientController(
    private val authenticatedApolloWrapper: AuthenticatedApolloWrapper,
    private val billingClientWrapper: BillingClientWrapper,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val settingsDataModel: SettingsDataModel,
) {
    internal data class BillingClientJob(
        val uniqueJobName: String,
        val job: suspend (assumeFailureForNow: Boolean) -> Boolean
    )

    private val activeJobSet: MutableSet<String> = mutableSetOf()
    // There are only 2 tasks that should ever be added to the job queue.
    internal val jobQueueFlow: MutableSharedFlow<BillingClientJob> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 3
    )

    val fetchPurchasesManager = FetchPurchasesManager(
        billingClientController = this,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    )
    val fetchProductDetailsManager = FetchProductDetailsManager(
        this,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    )

    // Flows
    val purchasesFlow: StateFlow<List<Purchase>?> = fetchPurchasesManager.purchasesFlow
    val productDetailsFlowWrapper: StateFlow<ProductDetailsWrapper> =
        fetchProductDetailsManager.productDetailsFlow
    private val _obfuscatedUserIDFlow: MutableStateFlow<String?> = MutableStateFlow(null)
    val obfuscatedUserIDFlow: StateFlow<String?> = _obfuscatedUserIDFlow

    private var attempts = 0

    internal fun getBillingClient(): BillingClient? {
        return billingClientWrapper.billingClient
    }

    init {
        billingClientWrapper.setUpPurchasesUpdatedListener(fetchPurchasesManager)
        coroutineScope.launch(dispatchers.io) {
            // Process one job at a time so multiple BillingClient connection retries don't happen
            // at the same time.
            jobQueueFlow.collect {
                processJob(it)
            }
        }
    }

    fun onResume() {
        attempts = 0
        // The Google Play Billing Integration docs say to do this every onResume:
        // https://developer.android.com/google/play/billing/integrate#fetch
        fetchProductsAndPurchases()
    }

    fun onDestroy() {
        billingClientWrapper.terminateBillingConnection()
    }

    private fun fetchProductsAndPurchases() {
        queueJobWithRetry(
            uniqueJobName = "fetchPurchases",
            job = fetchPurchasesManager::fetchPurchases
        )
        queueJobWithRetry(
            uniqueJobName = "fetchProductDetails",
            job = fetchProductDetailsManager::fetchProductDetails
        )
    }

    /**
     * Queues a job. If the job fails, it will restart the billing connection and retry the job a
     * maximum of [MAX_ATTEMPTS] times with exponential backoff.
     *
     * @param [job] should return if the [job] succeeded or not.
     */
    internal fun queueJobWithRetry(uniqueJobName: String, job: suspend (Boolean) -> Boolean) {
        coroutineScope.launch(dispatchers.io) {
            synchronized(activeJobSet) {
                val hasBeenAdded = activeJobSet.add(uniqueJobName)
                if (!hasBeenAdded) {
                    return@launch
                }
            }
            jobQueueFlow.emit(BillingClientJob(uniqueJobName = uniqueJobName, job = job))
        }
    }

    /** Processes one [BillingClientJob] job at a time until the [jobQueue] is empty. */
    private suspend fun processJob(billingClientJob: BillingClientJob) {
        var success = billingClientJob.job(false)
        attempts += 1
        while (!success && attempts < MAX_ATTEMPTS) {
            delay(getDelay(attempts))
            if (billingClientWrapper.retryConnection()) {
                success = billingClientJob.job(attempts >= ATTEMPTS_TO_ASSUME_FAILURE)
            }
            attempts += 1
        }

        if (success) {
            attempts = 0
        } else {
            Timber.e("Dropping ${billingClientJob.uniqueJobName} job after $MAX_ATTEMPTS retries.")
        }

        synchronized(activeJobSet) {
            activeJobSet.remove(billingClientJob.uniqueJobName)
        }
    }

    private fun getDelay(attempts: Int): Long {
        return when (attempts) {
            1 -> 0
            2 -> FIRST_RETRY_WAIT_TIME
            else -> 1000L * RETRY_WAIT_TIME_MULTIPLIER.pow(attempts - 1).toLong()
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        billingParams: BillingFlowParams,
        onBillingFlowFinished: (BillingResult) -> Unit
    ) {
        fetchPurchasesManager.setOnPurchasesCallback {
            onBillingFlowFinished(it)
        }

        val billingResult = billingClientWrapper.billingClient
            ?.launchBillingFlow(activity, billingParams)

        if (billingResult?.responseCode != BillingResponseCode.OK) {
            Timber.e("Error in launching billing flow. ${billingResult?.debugMessage}")
        }
    }

    fun onUserSignedIn() {
        if (!settingsDataModel.getToggleState(SettingsToggle.DEBUG_ENABLE_BILLING).value) {
            return
        }
        coroutineScope.launch(dispatchers.io) {
            val response = authenticatedApolloWrapper.performMutation(
                mutation = InitializeGooglePlaySubscriptionMutation(),
                userMustBeLoggedIn = true
            ).response?.data?.initializeGooglePlaySubscription
            _obfuscatedUserIDFlow.value = response?.obfuscatedUserID
            // TODO(kobec): add proper error handling here: https://github.com/neevaco/neeva-android/issues/1197
            if (obfuscatedUserIDFlow.value == null) {
                Log.e(
                    "ObfuscatedUserId",
                    "InitializeGooglePlaySubscriptionMutation response = $response"
                )
            }
        }
    }

    companion object {
        // region Retrying BillingClient connection parameters
        const val SLOW_CONNECTION_WAIT_TIME = 1000L * 60
        const val MAX_ATTEMPTS = 5
        const val FIRST_RETRY_WAIT_TIME = 300L
        const val RETRY_WAIT_TIME_MULTIPLIER = 2.0
        // If a job fails this many times, we will assume failure for now. We will continue to try the
        // job in the background until the max attempts have been finished.
        const val ATTEMPTS_TO_ASSUME_FAILURE = 4
        // endregion
    }
}
