// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.settings.sharedcomposables.subcomponents.SettingsLinkRow
import com.neeva.app.type.SubscriptionType

@Composable
fun SubscriptionRow(
    subscriptionType: SubscriptionType?,
    openUrl: () -> Unit
) {
    val subscriptionString = GetSubscriptionString(subscriptionType)
    SettingsLinkRow(
        label = subscriptionString,
        openUrl = openUrl
    )
}

@Composable
fun GetSubscriptionString(subscriptionType: SubscriptionType?): String {
    return when (subscriptionType) {
        SubscriptionType.Basic -> stringResource(id = R.string.subscription_type_basic)
        SubscriptionType.Premium -> stringResource(id = R.string.subscription_type_premium)
        SubscriptionType.Unlimited -> stringResource(id = R.string.subscription_type_unlimited)
        else -> stringResource(id = R.string.subscription_type_unknown)
    }
}
