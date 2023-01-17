// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.neeva.app.Dispatchers
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.billing.BillingSubscriptionPlanTags.SUB_PRODUCT_ID
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.userdata.NeevaUser
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Provides everything needed to manage and purchase subscriptions.
 * Exposes [ProductDetails] and [Purchase] states.
 */
class SubscriptionManager(
    private val appContext: Context,
    private val activityStarter: ActivityStarter,
    private val billingClientController: BillingClientController,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaUser: NeevaUser,
    private val sharedPreferencesModel: SharedPreferencesModel
) {
    val selectedSubscriptionTag: String
        get() = SharedPrefFolder.FirstRun.SelectedSubscriptionTag.get(sharedPreferencesModel)
    val selectedSubscriptionTagFlow: StateFlow<String>
        get() = SharedPrefFolder.FirstRun.SelectedSubscriptionTag.getFlow(sharedPreferencesModel)

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

    fun isPremiumPurchaseAvailable(): Boolean {
        val offers = productDetailsFlow.value?.subscriptionOfferDetails
        val existingPurchases = purchasesFlow.value
        // TODO(kobec): add obfuscatedUserID != null &&
        //  check SubscriptionSource == GooglePlayStore or null
        return existingPurchases.isNullOrEmpty() || offers != null
    }

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
     * Use the Google Play Billing Library to make a purchase. If the FREE [tag] is passed in, this
     * function will do nothing.
     */
    private fun buy(
        activityReference: WeakReference<Activity>,
        tag: String?,
        onBillingFlowFinished: (BillingResult) -> Unit
    ) {
        val productDetails = productDetailsFlow.value
        if (productDetails == null) {
            Timber.e("Unable to launch Purchase Flow because product details is null.")
            return
        }

        if (
            tag != BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN &&
            tag != BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN
        ) {
            return
        }

        val offerDetails = retrieveEligibleOffers(
            offerDetails = productDetails.subscriptionOfferDetails,
            tag = tag.lowercase()
        )
        val offerToken = leastPricedOfferToken(offerDetails)
        val obfuscatedUserID = billingClientController.obfuscatedUserIDFlow.value

        if (isPremiumPurchaseAvailable()) {
            offerToken?.let {
                val billingParams = billingFlowParamsBuilder(
                    productDetails = productDetails,
                    offerToken = it,
                    obfuscatedUserID = obfuscatedUserID
                )
                activityReference.get()?.let {
                    billingClientController.launchBillingFlow(
                        activity = it,
                        billingParams = billingParams.build(),
                        onBillingFlowFinished = onBillingFlowFinished
                    )
                }
            }
        }
    }

    fun queueBuyPremiumOnSignInIfSelected(
        activityReference: WeakReference<Activity>,
        onBillingFlowFinished: (BillingResult) -> Unit
    ) {
        selectedSubscriptionTag
            .takeIf { it.isNotEmpty() }
            ?.let {
                neevaUser.queueOnSignIn(uniqueJobName = QUEUE_BUY_ON_SIGN_IN_JOB_NAME) {
                    coroutineScope.launch(dispatchers.main) {
                        buy(
                            activityReference = activityReference,
                            tag = it,
                            onBillingFlowFinished = { billingResult ->
                                onBillingFlowFinished(billingResult)
                                clearSelectedSubscriptionTag()
                            }
                        )
                    }
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

    fun setSubscriptionTagForPremiumPurchase(tag: String?) {
        SharedPrefFolder.FirstRun.SelectedSubscriptionTag.set(
            sharedPreferencesModel,
            tag ?: "",
            mustCommitImmediately = true
        )
    }

    fun clearSelectedSubscriptionTag() {
        SharedPrefFolder.FirstRun.SelectedSubscriptionTag.set(sharedPreferencesModel, "")
    }

    companion object {
        val QUEUE_BUY_ON_SIGN_IN_JOB_NAME = "WelcomeFlow: QueueBuyOnSuccessfulSignIn"
    }
}
