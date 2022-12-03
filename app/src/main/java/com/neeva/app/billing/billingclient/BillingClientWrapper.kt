// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing.billingclient

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.neeva.app.Dispatchers
import javax.annotation.concurrent.NotThreadSafe
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/** Provides an API to properly manage the [BillingClient] setup. */
@NotThreadSafe
class BillingClientWrapper(
    val appContext: Context,
    val coroutineScope: CoroutineScope,
    val dispatchers: Dispatchers,
) {
    internal var billingClient: BillingClient? = null
    internal lateinit var purchasesUpdatedListener: PurchasesUpdatedListener

    // Deferring the setup of this listener allows us to decouple this class from
    // BillingClientController.
    internal fun setUpPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
        purchasesUpdatedListener = listener
    }

    private fun createNewBillingClient(): BillingClient {
        return BillingClient.newBuilder(appContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
    }

    private suspend fun startBillingConnection(): Boolean {
        return suspendCoroutine { continuation ->
            billingClient.let {
                if (it == null) {
                    continuation.resume(false)
                    return@suspendCoroutine
                }

                it.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            continuation.resume(true)
                        } else {
                            continuation.resume(false)
                            Timber.e(errorString(billingResult))
                        }
                    }
                    override fun onBillingServiceDisconnected() {
                        continuation.resume(false)
                        // This function is not reliable.
                        // It does not always get called when the billing service is disconnected.
                        Timber.e("onBillingServiceDisconnected()")
                    }
                })
            }
        }
    }

    internal fun terminateBillingConnection() {
        // Although this isn't guaranteed be called, if the BillingClient connection is invalid,
        // a future use of BillingClient (i.e. fetchPurchases or fetchProductDetails) will have an
        // error. This will trigger the old, invalid billingClient to be destroyed and replaced by a
        // new one.
        billingClient?.endConnection()
    }

    internal suspend fun retryConnection(): Boolean {
        // BillingClient can only be used once.
        // After calling endConnection(), we must create a new BillingClient.
        // See: https://github.com/android/play-billing-samples/blob/main/ClassyTaxiJava/app/src/main/java/com/sample/android/classytaxijava/billing/BillingClientLifecycle.java#L115
        terminateBillingConnection()
        billingClient = createNewBillingClient()
        return startBillingConnection()
    }

    private fun errorString(billingResult: BillingResult): String {
        val docsUrl = "https://developer.android.com/reference/com/android/billingclient/api/" +
            "BillingClient.BillingResponseCode"
        return "BillingSetup failed with response code = " +
            "${billingResult.responseCode}, " +
            "debugMessage = ${billingResult.debugMessage} " +
            "For more information see: $docsUrl"
    }
}
