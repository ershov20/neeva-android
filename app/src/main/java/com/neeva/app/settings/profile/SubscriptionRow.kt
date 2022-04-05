package com.neeva.app.settings.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsLinkRow
import com.neeva.app.type.SubscriptionType

@Composable
fun SubscriptionRow(
    subscriptionType: SubscriptionType?,
    openUrl: () -> Unit
) {
    val subscriptionString = when (subscriptionType) {
        SubscriptionType.Basic -> stringResource(id = R.string.subscription_basic)

        SubscriptionType.Premium -> stringResource(id = R.string.subscription_premium)

        else -> stringResource(id = R.string.subscription_unknown)
    }
    SettingsLinkRow(
        label = subscriptionString,
        openUrl = openUrl
    )
}
