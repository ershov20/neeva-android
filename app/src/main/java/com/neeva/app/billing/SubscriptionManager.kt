// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.billing.BillingSubscriptionPlanTags.SUB_PRODUCT_ID
import com.neeva.app.billing.billingclient.BillingClientController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Provides everything needed to manage and purchase subscriptions.
 * Exposes [ProductDetails] and [Purchase] states.
 */
class SubscriptionManager(
    private val appContext: Context,
    private val activityStarter: ActivityStarter,
    private val billingClientController: BillingClientController,
) {
    val productDetailsFlow = billingClientController.productDetailsFlow
    val purchasesFlow = billingClientController.purchasesFlow
    val obfuscatedUserIDFlow = billingClientController.obfuscatedUserIDFlow

    val hasAnnualPremiumPlan: Flow<Boolean> = billingClientController.purchasesFlow
        .map { purchaseList ->
            purchaseList.any { purchase ->
                purchase.products.contains(BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN)
            }
        }
        .distinctUntilChanged()

    val hasMonthlyPremiumPlan: Flow<Boolean> = billingClientController.purchasesFlow
        .map { purchaseList ->
            purchaseList.any { purchase ->
                purchase.products.contains(BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN)
            }
        }
        .distinctUntilChanged()

    /**
     * Retrieves all eligible base plans and offers using tags from ProductDetails.
     *
     * @param offerDetails offerDetails from a ProductDetails returned by the library.
     * @param tag string representing tags associated with offers and base plans.
     *
     * @return the eligible offers and base plans in a list.
     *
     */
    private fun retrieveEligibleOffers(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>?,
        tag: String
    ): List<ProductDetails.SubscriptionOfferDetails>? {
        if (offerDetails == null) {
            Timber.e("OfferDetails is null or empty so could not retrieve eligible offers.")
            return null
        }

        return offerDetails.filter { it.offerTags.contains(tag) }
    }

    /**
     * Calculates the lowest-priced offer amongst all eligible offers.
     * In this implementation the lowest price of all offers' pricing phases is returned.
     * It's possible the logic can be implemented differently.
     * For example, the lowest average price in terms of month could be returned instead.
     *
     * @param offerDetails List of of eligible offers and base plans.
     *
     * @return the offer id token of the lowest priced offer.
     */
    private fun leastPricedOfferToken(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>?
    ): String? {
        if (offerDetails.isNullOrEmpty()) {
            Timber.e("OfferDetails is null or empty so could not find an offer token.")
            return null
        }

        return offerDetails
            .associateBy(
                { it.offerToken },
                { offer ->
                    offer.pricingPhases.pricingPhaseList.minBy { it.priceAmountMicros }
                }
            )
            .minBy { it.value.priceAmountMicros }
            .key
    }

    private fun billingFlowParamsBuilder(
        productDetails: ProductDetails,
        offerToken: String,
        obfuscatedUserID: String?
    ): BillingFlowParams.Builder {
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            ).apply {
                if (obfuscatedUserID != null) {
                    setObfuscatedAccountId(obfuscatedUserID)
                } else {
                    Timber.e("ObfuscatedAccountID is null!")
                }
            }
    }

    /**
     * Use the Google Play Billing Library to make a purchase.
     *
     * @param productDetails ProductDetails object returned by the library.
     * @param existingPurchases List of current [Purchase] objects needed for upgrades or downgrades.
     * @param billingClient Instance of [BillingClientController].
     * @param activity [Activity] instance.
     * @param tag String representing tags associated with offers and base plans.
     */
    fun buy(activity: Activity, tag: String) {
        val productDetails = productDetailsFlow.value
        if (productDetails == null) {
            Timber.e("Unable to launch Purchase Flow because product details is null.")
            return
        }
        val existingPurchases = purchasesFlow.value

        val offerDetails = retrieveEligibleOffers(
            offerDetails = productDetails.subscriptionOfferDetails,
            tag = tag.lowercase()
        )
        val offerToken = leastPricedOfferToken(offerDetails)
        val obfuscatedUserID = billingClientController.obfuscatedUserIDFlow.value

        // TODO(kobec): add obfuscatedUserID != null &&
        //  check SubscriptionSource == GooglePlayStore or null
        if (existingPurchases.isNullOrEmpty()) {
            offerToken?.let {
                val billingParams = billingFlowParamsBuilder(
                    productDetails = productDetails,
                    offerToken = it,
                    obfuscatedUserID = obfuscatedUserID
                )
                billingClientController.launchBillingFlow(activity, billingParams.build())
            }
        }
    }

    fun manageSubscriptions() {
        val uri = Uri.parse(
            "https://play.google.com/store/account/subscriptions?" +
                "sku=$SUB_PRODUCT_ID&" +
                "package=${appContext.packageName}"
        )
        activityStarter.safeStartActivityForIntent(Intent(Intent.ACTION_VIEW, uri))
    }
}
