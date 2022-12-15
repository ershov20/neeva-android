// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.neeva.app.LocalNeevaUser
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.LocalSubscriptionManager
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginFlowParams
import com.neeva.app.firstrun.LegalFooter
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.type.SubscriptionType
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.userdata.NeevaUser

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

        Spacer(Modifier.height(64.dp))

        ContinueButton(
            navigateToPlans = navigateToPlans,
            navigateToSetDefaultBrowser = navigateToSetDefaultBrowser
        )

        Spacer(Modifier.height(10.dp))

        LoginButton(
            signup = false,
            navigateToPlans = navigateToPlans,
            navigateToSetDefaultBrowser = navigateToSetDefaultBrowser
        )

        Spacer(Modifier.height(Dimensions.PADDING_LARGE))

        ConsentCheckbox(
            loggingConsentState = loggingConsentState,
            toggleLoggingConsentState = toggleLoggingConsentState
        )

        LegalFooter(
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth(),
            onOpenUrl = { uri -> firstRunModel.openInCustomTabs(context, uri) }
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

// TODO(kobec): I've only wrote the skeleton, but will be unable to confirm if it works when CCT is
//  working.
@Composable
internal fun LoginButton(
    signup: Boolean,
    navigateToPlans: () -> Unit,
    navigateToSetDefaultBrowser: () -> Unit
) {
    // TODO(kobec): Finish this function when CCT is fixed.
    val firstRunModel = LocalFirstRunModel.current
    val context = LocalContext.current
    val neevaUser = LocalNeevaUser.current

    val params = LaunchLoginFlowParams(
        provider = NeevaUser.SSOProvider.OKTA,
        signup = signup
    )
    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        firstRunModel.handleLoginActivityResult(context, result, params) {
            neevaUser.queueOnSignIn(uniqueJobName = "Welcome Flow: onSuccessfulSignIn") {
                val userInfo = neevaUser.userInfoFlow.value
                val subscriptionType = userInfo?.subscriptionType
                if (subscriptionType != null && subscriptionType == SubscriptionType.Basic) {
                    navigateToPlans()
                } else {
                    navigateToSetDefaultBrowser()
                }
            }
        }
    }

    Button(
        onClick = {
            firstRunModel.launchLoginFlow(
                context = context,
                launchLoginFlowParams = LaunchLoginFlowParams(
                    provider = NeevaUser.SSOProvider.OKTA,
                    signup = false
                ),
                activityResultLauncher = resultLauncher
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.welcomeflow_i_have_an_account),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(Dimensions.PADDING_MEDIUM)
        )
    }
}

@Composable
fun ContinueButton(navigateToPlans: () -> Unit, navigateToSetDefaultBrowser: () -> Unit) {
    val subscriptionManager = LocalSubscriptionManager.current
    val subscriptionOfferDetails = subscriptionManager.productDetailsFlow
        .collectAsState().value?.subscriptionOfferDetails

    val onClick = if (subscriptionOfferDetails.isNullOrEmpty()) {
        navigateToSetDefaultBrowser
    } else {
        navigateToPlans
    }

    WelcomeFlowButton(
        primaryText = stringResource(id = R.string.welcomeflow_lets_go),
        onClick = onClick
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
