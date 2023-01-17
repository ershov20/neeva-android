// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing

object BillingSubscriptionPlanTags {
    // region Used in this project and Google Play Console
    const val SUB_PRODUCT_ID = "premium"
    const val ANNUAL_PREMIUM_PLAN = "annual-premium"
    const val MONTHLY_PREMIUM_PLAN = "monthly-premium"
    // endregion

    // Used in this project
    const val FREE_PLAN = "free"

    fun isPremiumPlanTag(tag: String): Boolean {
        return tag == ANNUAL_PREMIUM_PLAN || tag == MONTHLY_PREMIUM_PLAN
    }
}
