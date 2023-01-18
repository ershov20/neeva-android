// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing.billingclient

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.neeva.app.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class FetchPurchasesManager(
    val billingClientController: BillingClientController,
    val coroutineScope: CoroutineScope,
    val dispatchers: Dispatchers
) : PurchasesUpdatedListener {
    private val _purchasesFlow: MutableStateFlow<List<Purchase>?> = MutableStateFlow(null)
    val purchasesFlow: StateFlow<List<Purchase>?> = _purchasesFlow

    private val params = QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.SUBS)
        .build()

    private var callback: (BillingResult) -> Unit = { }

    private val slowInternetConnectionTimer: Job

    init {
        slowInternetConnectionTimer = coroutineScope.launch(dispatchers.io) {
            delay(BillingClientController.SLOW_CONNECTION_WAIT_TIME)
            assumeFailureToFetch()
        }
    }

    suspend fun fetchPurchases(
        shouldAssumeFailureForNow: Boolean
    ): Boolean = suspendCoroutine { fetchContinuation ->
        billingClientController.getBillingClient().let {
            if (it == null) {
                Timber.e("BillingClient is null. Was not able to fetch purchases.")
                fetchContinuation.resume(false)
                return@suspendCoroutine
            }

            it.queryPurchasesAsync(params) { billingResult, purchaseList ->
                if (
                    billingResult.responseCode == BillingClient.BillingResponseCode.OK
                ) {
                    slowInternetConnectionTimer.cancel()
                    fetchContinuation.resume(true)
                    // If the BillingClient's last connection is cached, it is possible that
                    // this purchasesList returned is outdated.
                    // But that's okay because onPurchasesUpdated() will return a new purchase
                    // list that should be up to date.
                    _purchasesFlow.value = purchaseList
                } else {
                    fetchContinuation.resume(false)
                    if (shouldAssumeFailureForNow) {
                        assumeFailureToFetch()
                    }
                    Timber.e(
                        "queryPurchases has failed with code ${billingResult.responseCode}. " +
                            billingResult.debugMessage
                    )
                }
            }
        }
    }

    fun assumeFailureToFetch() {
        _purchasesFlow.value = emptyList()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (
            billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            !purchases.isNullOrEmpty()
        ) {
            _purchasesFlow.value = purchases
            purchases.forEach { purchase ->
                // TODO(kobec): Verify the purchaseToken first with BE!
                Timber.e("purchase token acquired: ${purchase.purchaseToken}")
                if (!purchase.isAcknowledged) {
                    billingClientController.queueJobWithRetry(
                        uniqueJobName = "acknowledgePurchase",
                        job = {
                            acknowledgePurchase(purchase)
                        }
                    )
                }
            }
        } else {
            Timber.e(
                "onPurchasesUpdated() had an error with code = ${billingResult.responseCode}. " +
                    billingResult.debugMessage
            )
        }
        callback(billingResult)
        setOnPurchasesCallback {}
    }

    fun setOnPurchasesCallback(newCallback: (BillingResult) -> Unit) {
        callback = newCallback
    }

    /** Perform new subscription purchases' acknowledgement on the client side. */
    private suspend fun acknowledgePurchase(purchase: Purchase): Boolean =
        suspendCoroutine { continuation ->
            val billingClient = billingClientController.getBillingClient()
            if (billingClient == null) {
                Timber.e("BillingClient was null. Unable to acknowledge purchase.")
                continuation.resume(false)
                return@suspendCoroutine
            }

            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            Timber.e("Purchase token acquired: ${purchase.purchaseToken}")

            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    continuation.resume(true)
                    // TODO(kobec): Since purchase has been acknowledged,
                    //  grant the purchase to the user
                    // _isNewPurchaseAcknowledged.value = true
                    Timber.e("Purchase has been acknowledged.")
                } else {
                    continuation.resume(false)
                    Timber.e(
                        "acknowledgePurchase() had an error with code: " +
                            billingResult.responseCode + ". " + billingResult.debugMessage
                    )
                }
            }
        }
}
