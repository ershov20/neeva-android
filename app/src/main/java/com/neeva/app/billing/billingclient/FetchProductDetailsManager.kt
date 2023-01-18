// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing.billingclient

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.neeva.app.Dispatchers
import com.neeva.app.billing.BillingSubscriptionPlanTags.SUB_PRODUCT_ID
import com.neeva.app.billing.billingclient.BillingClientController.Companion.SLOW_CONNECTION_WAIT_TIME
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class ProductDetailsWrapper(
    val productDetails: ProductDetails? = null,
    val isSet: Boolean = true
)

class FetchProductDetailsManager(
    private val billingClientController: BillingClientController,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers
) {
    private val _productDetailsFlow = MutableStateFlow(ProductDetailsWrapper(isSet = false))
    val productDetailsFlow: StateFlow<ProductDetailsWrapper> = _productDetailsFlow

    private val params = QueryProductDetailsParams.newBuilder()
        .setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUB_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        )
    private val slowInternetConnectionTimer: Job

    init {
        slowInternetConnectionTimer = coroutineScope.launch(dispatchers.io) {
            delay(SLOW_CONNECTION_WAIT_TIME)
            assumeFailureToFetch()
        }
    }

    suspend fun fetchProductDetails(
        shouldAssumeFailureForNow: Boolean
    ): Boolean = suspendCoroutine { fetchContinuation ->
        billingClientController.getBillingClient().let {
            if (it == null) {
                Timber.e("BillingClient is null. Was not able to fetch product details.")
                fetchContinuation.resume(false)
                return@suspendCoroutine
            }

            it.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->
                if (
                    billingResult.responseCode == BillingResponseCode.OK &&
                    productDetailsList.isNotEmpty()
                ) {
                    slowInternetConnectionTimer.cancel()
                    fetchContinuation.resume(true)
                    _productDetailsFlow.value = ProductDetailsWrapper(
                        productDetails = productDetailsList.find { product ->
                            product.productId == SUB_PRODUCT_ID
                        }
                    )
                } else {
                    fetchContinuation.resume(false)
                    if (shouldAssumeFailureForNow) {
                        assumeFailureToFetch()
                    }
                    Timber.e(
                        "onProductDetailsResponse: ${billingResult.responseCode}. " +
                            billingResult.debugMessage
                    )
                }
            }
        }
    }

    fun assumeFailureToFetch() {
        _productDetailsFlow.value = ProductDetailsWrapper(
            productDetails = null,
            isSet = true
        )
    }
}
