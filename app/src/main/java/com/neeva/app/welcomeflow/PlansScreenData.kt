// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.neeva.app.R
import com.neeva.app.billing.BillingSubscriptionPlanTags
import java.text.NumberFormat
import java.util.Currency

data class Benefits(
    val title: String,
    val description: String? = null
)

data class Pricing(
    val monthlyPrice: String,
    val annualPrice: String? = null,
)

data class SubscriptionPlan(
    val tag: String,
    val name: String,
    val benefits: List<Benefits>,
    val price: Pricing? = null
)

object PlansScreenData {
    @Composable
    private fun getPremiumBenefits(): List<Benefits> {
        return listOf(
            Benefits(stringResource(id = R.string.welcomeflow_browser_benefit)),
            Benefits(stringResource(id = R.string.welcomeflow_premium_ad_free_search_benefit)),
            Benefits(stringResource(id = R.string.welcomeflow_premium_unlimited_devices_benefit)),
            Benefits(stringResource(id = R.string.welcomeflow_password_vpn_benefit)),
        )
    }

    @Composable
    private fun getFreeBenefits(): List<Benefits> {
        return listOf(
            Benefits(stringResource(id = R.string.welcomeflow_browser_benefit)),
            Benefits(
                stringResource(id = R.string.welcomeflow_limited_ad_free_search_benefit),
                stringResource(id = R.string.welcomeflow_limited_ad_free_search_benefit_description)
            ),
            Benefits(stringResource(id = R.string.welcomeflow_limited_devices_benefit)),
        )
    }

    @Composable
    fun getSubscriptionPlans(
        subscriptionOfferDetails: List<SubscriptionOfferDetails>,
        showFreePlan: Boolean
    ): List<SubscriptionPlan> {
        return mutableListOf<SubscriptionPlan>().apply {
            if (showFreePlan) {
                add(
                    SubscriptionPlan(
                        tag = BillingSubscriptionPlanTags.FREE_PLAN,
                        name = stringResource(id = R.string.welcomeflow_free_plan),
                        benefits = getFreeBenefits()
                    )
                )
            }

            val annualPlan = getSubscriptionPlan(
                subscriptionOfferDetails = subscriptionOfferDetails,
                tag = BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN
            )

            if (annualPlan != null) {
                add(
                    SubscriptionPlan(
                        tag = BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN,
                        name = stringResource(id = R.string.welcomeflow_annual_premium_plan),
                        benefits = getPremiumBenefits(),
                        price = Pricing(
                            monthlyPrice = yearlyPriceToMonthly(annualPlan),
                            annualPrice = getFormattedPrice(annualPlan)
                        )
                    )
                )
            }

            val monthlyPlan = getSubscriptionPlan(
                subscriptionOfferDetails = subscriptionOfferDetails,
                tag = BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN
            )
            if (monthlyPlan != null) {
                add(
                    SubscriptionPlan(
                        tag = BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN,
                        name = stringResource(id = R.string.welcomeflow_monthly_premium_plan),
                        benefits = getPremiumBenefits(),
                        price = Pricing(monthlyPrice = getFormattedPrice(monthlyPlan))
                    )
                )
            }
        }
    }

    private fun getSubscriptionPlan(
        subscriptionOfferDetails: List<SubscriptionOfferDetails>,
        tag: String
    ): SubscriptionOfferDetails? {
        return subscriptionOfferDetails.find {
            it.offerTags.contains(tag)
        }
    }

    private fun getFormattedPrice(subscriptionOfferDetails: SubscriptionOfferDetails): String {
        // Since the free trial is one of the pricing phases, use the recurring price as the
        // formatted price.
        return subscriptionOfferDetails.pricingPhases.pricingPhaseList
            .maxBy { it.priceAmountMicros }.formattedPrice
    }

    private fun yearlyPriceToMonthly(offerDetails: SubscriptionOfferDetails): String {
        val pricingPhase = offerDetails.pricingPhases.pricingPhaseList
            .maxBy { it.priceAmountMicros }

        // 1,000,000 micro-units is equal to one unit of the currency.
        // https://developer.android.com/reference/com/android/billingclient/api/SkuDetails.html#getPriceAmountMicros()
        val microUnitsMultiplier = 1000000

        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(pricingPhase.priceCurrencyCode)
        return format.format(pricingPhase.priceAmountMicros / (12.0 * microUnitsMultiplier))
    }

    @Composable
    internal fun getPreviewSubscriptionPlans(): List<SubscriptionPlan> {
        return listOf(
            SubscriptionPlan(
                tag = BillingSubscriptionPlanTags.FREE_PLAN,
                name = stringResource(id = R.string.welcomeflow_free_plan),
                benefits = getFreeBenefits()
            ),
            SubscriptionPlan(
                tag = BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN,
                name = stringResource(id = R.string.welcomeflow_annual_premium_plan),
                benefits = getPremiumBenefits(),
                price = Pricing(monthlyPrice = "$4.17", annualPrice = "$49.99")
            ),
            SubscriptionPlan(
                tag = BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN,
                name = stringResource(id = R.string.welcomeflow_monthly_premium_plan),
                benefits = getPremiumBenefits(),
                price = Pricing(monthlyPrice = "$5.99")
            )
        )
    }
}
