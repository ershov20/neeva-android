// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.neeva.app.LocalSubscriptionManager
import com.neeva.app.billing.BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN
import com.neeva.app.billing.BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN
import com.neeva.app.firstrun.widgets.buttons.CloseButton
import com.neeva.app.settings.SettingsToggle
import timber.log.Timber

/**
 * This screen is just a placeholder to prototype the Billing Flow.
 * It is currently sand-boxed by [SettingsToggle.DEBUG_ENABLE_BILLING].
 */
@Composable
fun BillingScreen(onDismiss: () -> Unit) {
    val activity = LocalContext.current as Activity
    val subscriptionManager = LocalSubscriptionManager.current

    val purchases = subscriptionManager.purchasesFlow.collectAsState().value
    val productDetails = subscriptionManager.productDetailsFlow.collectAsState().value
    val obfuscatedUserID = subscriptionManager.obfuscatedUserIDFlow.collectAsState().value
    val hasMonthlyPremiumPlan = subscriptionManager
        .hasMonthlyPremiumPlan.collectAsState(false).value
    val hasAnnualPremiumPlan = subscriptionManager
        .hasAnnualPremiumPlan.collectAsState(false).value

    Timber.tag("KOBE").e("obfuscatedUserID = $obfuscatedUserID")
    Timber.tag("KOBE").e("purchases = $purchases")
    Timber.tag("KOBE").e("product details = $productDetails")

    Surface {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                val subscription = subscriptionManager.let {
                    when {
                        hasMonthlyPremiumPlan -> "monthly premium"
                        hasAnnualPremiumPlan -> "annual premium"
                        else -> "free tier"
                    }
                }

                Text("Current subscription = $subscription")

                Text(
                    "obfuscatedUserID = $obfuscatedUserID"
                )

                for (purchase in purchases) {
                    Text(
                        "purchaseToken = ${purchase.purchaseToken}"
                    )
                    Timber.tag("KOBE").e("purchaseToken = ${purchase.purchaseToken}")
                }

                // Free
                Button(onClick = { /*TODO*/ }) {
                    Text("Free Plan")
                }

                // TODO(kobec): add check for obfuscatedUserID
                if (productDetails != null) {
                    // Monthly
                    Button(onClick = {
                        subscriptionManager.buy(
                            productDetails = productDetails,
                            existingPurchases = purchases,
                            tag = MONTHLY_PREMIUM_PLAN,
                            activity = activity
                        )
                    }) {
                        Text("Monthly Plan")
                    }

                    // Monthly
                    Button(onClick = {
                        subscriptionManager.buy(
                            productDetails = productDetails,
                            existingPurchases = purchases,
                            tag = ANNUAL_PREMIUM_PLAN,
                            activity = activity
                        )
                    }) {
                        Text("Annual Plan")
                    }
                }

                Button(onClick = {
                    subscriptionManager.manageSubscriptions()
                }) {
                    Text("Manage subscriptions")
                }
            }

            CloseButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
