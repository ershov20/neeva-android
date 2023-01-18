// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.neeva.app.Dispatchers
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.billing.BillingSubscriptionPlanTags.SUB_PRODUCT_ID
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.type.SubscriptionSource
import com.neeva.app.userdata.NeevaUser
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Provides everything needed to manage and purchase subscriptions.
 * Exposes [ProductDetails] and [Purchase] states.
 */
class SubscriptionManager(
    private val appContext: Context,
    private val activityStarter: ActivityStarter,
    private val billingClientController: BillingClientController,
    private val appCoroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaUser: NeevaUser,
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val settingsDataModel: SettingsDataModel,
) {
    // A scope intended only for a set of jobs related to activating a billing flow.
    private var subscriptionJob: Job? = null

    val selectedSubscriptionTag: String
        get() = SharedPrefFolder.FirstRun.SelectedSubscriptionTag.get(sharedPreferencesModel)
    val selectedSubscriptionTagFlow: StateFlow<String>
        get() = SharedPrefFolder.FirstRun.SelectedSubscriptionTag.getFlow(sharedPreferencesModel)

    val productDetailsWrapperFlow = billingClientController.productDetailsFlowWrapper
    val existingPurchasesFlow = billingClientController.purchasesFlow
    val isBillingAvailableFlow = productDetailsWrapperFlow
        .combine(existingPurchasesFlow) { productDetailsWrapper, existingPurchases ->
            if (!settingsDataModel.getSettingsToggleValue(SettingsToggle.DEBUG_ENABLE_BILLING)) {
                Log.v(
                    TAG,
                    "Billing not available because the dev `Enable billing` settings toggle " +
                        "is disabled."
                )
                return@combine false
            }

            // If the Billing information has not been fetched yet, then we still don't know if
            // Billing is available for this user.
            if (!productDetailsWrapper.isSet || existingPurchases == null) {
                Log.v(
                    TAG,
                    "Billing not available because product details or existing purchases have " +
                        "not been fetched yet. productDetails = $productDetailsWrapper, " +
                        "existingPurchases = $existingPurchases"
                )
                return@combine null
            }

            val offers = productDetailsWrapper.productDetails?.subscriptionOfferDetails
            if (offers.isNullOrEmpty() || existingPurchases.isNotEmpty()) {
                Log.v(
                    TAG,
                    "Billing not available because productDetails = $productDetailsWrapper, " +
                        "existingPurchases = $existingPurchases"
                )
                return@combine false
            }

            Log.v(TAG, "Google Billing API is now available to use.")
            true
        }
        .stateIn(appCoroutineScope, SharingStarted.Lazily, null)

    val obfuscatedUserIDFlow = billingClientController.obfuscatedUserIDFlow
    val isPremiumPurchaseAvailableFlow: StateFlow<Boolean?> = neevaUser.userInfoFlow
        .combine(obfuscatedUserIDFlow) { userInfo, obfuscatedUserID ->
            if (userInfo == null) {
                return@combine null
            }

            // TODO(kobec): Add this in when obfuscatedID is working:
            //  https://github.com/neevaco/neeva-android/issues/1197
//            if (obfuscatedUserID == null) {
//                Log.w("Billing", "Premium Purchase not ready. ObfuscatedID is null.")
//                return@combine null
//            }

            if (
                userInfo.subscriptionSource != SubscriptionSource.None &&
                userInfo.subscriptionSource != SubscriptionSource.GooglePlay
            ) {
                Log.w(
                    TAG,
                    "Premium Purchase is not available. " +
                        "SubscriptionSource = ${userInfo.subscriptionSource}."
                )
                return@combine false
            }

            Log.w(TAG, "Premium is ready for purchase.")
            return@combine true
        }
        .stateIn(appCoroutineScope, SharingStarted.Lazily, null)

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
            Log.w(TAG, "OfferDetails is null or empty so could not retrieve eligible offers.")
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
            Log.w(TAG, "OfferDetails is null or empty so could not find an offer token.")
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
                    Log.w(TAG, "ObfuscatedAccountID is null!")
                }
            }
    }

    private suspend fun collectIsPremiumPurchaseAvailable(): Boolean {
        return isPremiumPurchaseAvailableFlow
            .filter { it == true }
            .filterNotNull()
            .take(1)
            .single()
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
        subscriptionJob?.cancel()

        val productDetails = productDetailsWrapperFlow.value.productDetails
        if (productDetails == null) {
            Log.w(TAG, "Unable to launch Purchase Flow because product details is null.")
            return
        }

        if (
            tag != BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN &&
            tag != BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN
        ) {
            return
        }

        subscriptionJob = appCoroutineScope.launch(dispatchers.io) {
            val canPurchasePremium = collectIsPremiumPurchaseAvailable()

            if (canPurchasePremium) {
                val offerDetails = retrieveEligibleOffers(
                    offerDetails = productDetails.subscriptionOfferDetails,
                    tag = tag.lowercase()
                )

                val obfuscatedUserID = billingClientController.obfuscatedUserIDFlow.value
                // TODO(kobec): add proper error handling here and don't allow the purchase
                //  if obfuscatedUserID == null.
                //  https://github.com/neevaco/neeva-android/issues/1197

                leastPricedOfferToken(offerDetails)?.let {
                    val billingParams = billingFlowParamsBuilder(
                        productDetails = productDetails,
                        offerToken = it,
                        obfuscatedUserID = obfuscatedUserID
                    )

                    activityReference.get()?.let { activity ->
                        appCoroutineScope.launch(dispatchers.main) {
                            billingClientController.launchBillingFlow(
                                activity = activity,
                                billingParams = billingParams.build(),
                                onBillingFlowFinished = onBillingFlowFinished
                            )
                        }
                    }
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
        val TAG = "SubscriptionManager"
    }
}
