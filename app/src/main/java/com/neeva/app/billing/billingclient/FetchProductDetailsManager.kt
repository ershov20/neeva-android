// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing.billingclient

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.neeva.app.billing.BillingSubscriptionPlanTags.SUB_PRODUCT_ID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class FetchProductDetailsManager(private val billingClientController: BillingClientController) {
    private val _productDetailsFlow: MutableStateFlow<ProductDetails?> = MutableStateFlow(null)
    val productDetailsFlow: StateFlow<ProductDetails?> = _productDetailsFlow

    private val params = QueryProductDetailsParams.newBuilder()
        .setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUB_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        )

    suspend fun fetchProductDetails(): Boolean = suspendCoroutine { fetchContinuation ->
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
                    fetchContinuation.resume(true)
                    _productDetailsFlow.value = productDetailsList.find { product ->
                        product.productId == SUB_PRODUCT_ID
                    }
                } else {
                    fetchContinuation.resume(false)
                    Timber.e(
                        "onProductDetailsResponse: ${billingResult.responseCode}. " +
                            billingResult.debugMessage
                    )
                }
            }
        }
    }
}
