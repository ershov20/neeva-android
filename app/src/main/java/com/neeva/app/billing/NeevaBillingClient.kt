// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.neeva.app.Dispatchers
import com.neeva.app.settings.SettingsToggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** This class is currently incomplete! It is sandboxed by [SettingsToggle.DEBUG_ENABLE_BILLING]. */
class NeevaBillingClient(appContext: Context, val dispatchers: Dispatchers) {
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // TODO(kobec): finish function
        }

    private var billingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    // TODO(kobec): add actual subscription to the list of products
    private val queryProductDetailsParams =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("product_id_example")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

    private val _productDetailsListFlow = MutableStateFlow(listOf<ProductDetails>())
    val productDetailsListFlow: StateFlow<List<ProductDetails>> = _productDetailsListFlow
    // TODO(kobec): use this to expose to UI that billingClient is ready to be used.
    val isBillingClientReady = mutableStateOf(false)

    fun setUp() {
        isBillingClientReady.value = false
        startConnection {
            billingClient.queryProductDetailsAsync(
                queryProductDetailsParams
            ) { billingResult, productDetailsList ->
                when (billingResult.responseCode) {
                    BillingResponseCode.OK -> {
                        _productDetailsListFlow.value = productDetailsList
                        isBillingClientReady.value = true
                    }
                    // TODO(kobec): handle other error codes
                    else -> {
                        val errorMessage = "BillingClient ERROR: responseCode = " +
                            "${billingResult.responseCode} " + "${billingResult.debugMessage}"
                        Log.e(TAG, errorMessage)
                    }
                }
            }
        }
    }

    private fun startConnection(onConnectionEstablished: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    onConnectionEstablished()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                // TODO(kobec): check if there is an actual internet connection and add a listener
                //  for when there is one so we can startConnection again.
                // startConnection(onConnectionEstablished)
            }
        })
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        selectedOfferToken: String
    ) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(selectedOfferToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        // TODO(kobec): handle result
    }

    companion object {
        val TAG = NeevaBillingClient::class.simpleName
    }
}
