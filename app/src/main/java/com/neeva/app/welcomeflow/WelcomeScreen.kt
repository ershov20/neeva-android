// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.LocalSubscriptionManager
import com.neeva.app.R
import com.neeva.app.firstrun.LegalFooter
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.welcomeflow.login.launchLoginFlow

@Composable
fun WelcomeScreen(
    navigateToPlans: () -> Unit,
    navigateToSetDefaultBrowser: () -> Unit
) {
    val settingsDataModel = LocalSettingsDataModel.current
    WelcomeFlowContainer(headerText = stringResource(id = R.string.welcomeflow_initial_header)) {
        WelcomeScreenContent(
            navigateToPlans = navigateToPlans,
            navigateToSetDefaultBrowser = navigateToSetDefaultBrowser,
            loggingConsentState = settingsDataModel.getToggleState(SettingsToggle.LOGGING_CONSENT),
            toggleLoggingConsentState = settingsDataModel
                .getTogglePreferenceToggler(SettingsToggle.LOGGING_CONSENT),
            modifier = it
        )
    }
}

@Composable
fun WelcomeScreenContent(
    navigateToPlans: () -> Unit,
    navigateToSetDefaultBrowser: () -> Unit,
    loggingConsentState: MutableState<Boolean>,
    toggleLoggingConsentState: () -> Unit,
    modifier: Modifier
) {
    val firstRunModel = LocalFirstRunModel.current
    val context = LocalContext.current

    Column(modifier = modifier) {
        Spacer(Modifier.height(80.dp))

        MainBenefit(
            title = stringResource(id = R.string.welcomeflow_privacy_benefit),
            description = stringResource(id = R.string.welcomeflow_privacy_benefit_description)
        )

        Spacer(Modifier.height(18.dp))

        MainBenefit(
            title = stringResource(id = R.string.welcomeflow_unbiased_benefit),
            description = stringResource(id = R.string.welcomeflow_unbiased_benefit_description)
        )

        Spacer(Modifier.height(48.dp))

        ContinueButtons(
            navigateToPlans = navigateToPlans,
            navigateToSetDefaultBrowser = navigateToSetDefaultBrowser
        )

        ConsentCheckbox(
            loggingConsentState = loggingConsentState,
            toggleLoggingConsentState = toggleLoggingConsentState
        )

        LegalFooter(
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth(),
            onOpenUrl = { uri -> firstRunModel.openSingleTabActivity(context, uri) }
        )

        Spacer(Modifier.height(Dimensions.PADDING_HUGE))
    }
}

@Composable
internal fun ConsentCheckbox(
    loggingConsentState: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    toggleLoggingConsentState: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { toggleLoggingConsentState() }
    ) {
        Checkbox(
            checked = loggingConsentState.value,
            onCheckedChange = null,
            modifier = Modifier.size(Dimensions.SIZE_TOUCH_TARGET)
        )

        Text(
            text = stringResource(R.string.logging_consent),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ContinueButtons(navigateToPlans: () -> Unit, navigateToSetDefaultBrowser: () -> Unit) {
    val subscriptionManager = LocalSubscriptionManager.current
    val offers = subscriptionManager.productDetailsFlow.collectAsState().value
        ?.subscriptionOfferDetails

    WelcomeFlowStackedButtons(
        primaryText = stringResource(id = R.string.welcomeflow_lets_go),
        onPrimaryButton = {
            if (offers.isNullOrEmpty()) {
                navigateToSetDefaultBrowser()
            } else {
                navigateToPlans()
            }
        },
        secondaryText = stringResource(id = R.string.welcomeflow_i_have_an_account),
        onSecondaryButton = launchLoginFlow(
            activityToReturnTo = WelcomeFlowActivity::class.java.name,
            screenToReturnTo = WelcomeFlowActivity.Companion.Destinations.WELCOME.name,
            onPremiumAvailable = { navigateToPlans() },
            onPremiumUnavailable = { navigateToSetDefaultBrowser() }
        )
    )
}

@PortraitPreviews
@Composable
fun WelcomeScreen_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        WelcomeScreen(navigateToPlans = {}, navigateToSetDefaultBrowser = {})
    }
}

@PortraitPreviews
@Composable
fun WelcomeScreen_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        WelcomeScreen(navigateToPlans = {}, navigateToSetDefaultBrowser = {})
    }
}
