// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.login

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalSubscriptionManager
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.billing.BillingSubscriptionPlanTags
import com.neeva.app.firstrun.ActivityReturnParams
import com.neeva.app.firstrun.widgets.texts.EmailPromoCheckbox
import com.neeva.app.firstrun.widgets.texts.ToggleSignUpText
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.SecondaryWelcomeFlowButton
import com.neeva.app.welcomeflow.WelcomeFlowActivity
import com.neeva.app.welcomeflow.WelcomeFlowButtonContainer
import com.neeva.app.welcomeflow.WelcomeFlowContainer

@Composable
fun CreateAccountScreen(
    setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
    onShowOtherSignUpOptions: (() -> Unit)? = null,
    navigateToSignIn: () -> Unit,
    selectedSubscriptionPlanTag: String?,
    onBack: () -> Unit,
) {
    val firstRunModel = LocalFirstRunModel.current
    val subscriptionManager = LocalSubscriptionManager.current
    val activity = LocalContext.current as Activity
    val onPremiumAvailable = {
        if (
            selectedSubscriptionPlanTag != null &&
            selectedSubscriptionPlanTag != BillingSubscriptionPlanTags.FREE_PLAN
        ) {
            subscriptionManager.buy(activity, selectedSubscriptionPlanTag)
        }
    }

    val onPremiumUnavailable = { }

    val activityReturnParams =
        if (setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
            if (firstRunModel.mustShowFirstRun()) {
                ActivityReturnParams(
                    activityToReturnTo = WelcomeFlowActivity::class.java.name,
                    screenToReturnTo = WelcomeFlowActivity.Companion.Destinations
                        .FINISH_WELCOME_FLOW.name
                )
            } else {
                ActivityReturnParams(
                    activityToReturnTo = NeevaActivity::class.java.name,
                    screenToReturnTo = AppNavDestination.SETTINGS.name
                )
            }
        } else {
            ActivityReturnParams(
                activityToReturnTo = WelcomeFlowActivity::class.java.name,
                screenToReturnTo = WelcomeFlowActivity.Companion.Destinations
                    .SET_DEFAULT_BROWSER.name
            )
        }

    WelcomeFlowContainer(
        onBack = onBack,
        headerText = stringResource(id = R.string.welcomeflow_create_your_account)
    ) {
        Column(modifier = it) {
            Spacer(Modifier.height(96.dp))
            if (onShowOtherSignUpOptions != null) {
                SignUpWithGoogleButtons(
                    activityToReturnTo = activityReturnParams.activityToReturnTo,
                    screenToReturnTo = activityReturnParams.screenToReturnTo,
                    onOtherOptions = onShowOtherSignUpOptions,
                    onPremiumAvailable = onPremiumAvailable,
                    onPremiumUnavailable = onPremiumUnavailable
                )
            } else {
                SignUpWithOtherButtons(
                    activityToReturnTo = activityReturnParams.activityToReturnTo,
                    screenToReturnTo = activityReturnParams.screenToReturnTo,
                    onPremiumAvailable = onPremiumAvailable,
                    onPremiumUnavailable = onPremiumUnavailable
                )
            }
            Spacer(Modifier.height(32.dp))
            EmailPromoCheckbox(Modifier.padding(Dimensions.PADDING_MEDIUM))
            Spacer(Modifier.size(Dimensions.PADDING_LARGE))
            ToggleSignUpText(
                signup = true,
                onClick = navigateToSignIn
            )
        }
    }
}

@Composable
fun SignUpWithGoogleButtons(
    activityToReturnTo: String,
    screenToReturnTo: String,
    onOtherOptions: () -> Unit,
    onPremiumAvailable: () -> Unit,
    onPremiumUnavailable: () -> Unit
) {
    WelcomeFlowButtonContainer {
        LoginButton(
            activityToReturnTo = activityToReturnTo,
            screenToReturnTo = screenToReturnTo,
            provider = NeevaUser.SSOProvider.GOOGLE,
            signup = true,
            onPremiumAvailable = onPremiumAvailable,
            onPremiumUnavailable = onPremiumUnavailable
        )
        Spacer(Modifier.height(18.dp))
        SecondaryWelcomeFlowButton(text = stringResource(id = R.string.sign_up_other_options)) {
            onOtherOptions()
        }
    }
}

@Composable
fun SignUpWithOtherButtons(
    activityToReturnTo: String,
    screenToReturnTo: String,
    onPremiumAvailable: () -> Unit,
    onPremiumUnavailable: () -> Unit
) {
    WelcomeFlowButtonContainer {
        LoginButton(
            activityToReturnTo = activityToReturnTo,
            screenToReturnTo = screenToReturnTo,
            provider = NeevaUser.SSOProvider.GOOGLE,
            signup = false,
            onPremiumAvailable = onPremiumAvailable,
            onPremiumUnavailable = onPremiumUnavailable
        )
        Spacer(Modifier.height(18.dp))
        LoginButton(
            activityToReturnTo = activityToReturnTo,
            screenToReturnTo = screenToReturnTo,
            provider = NeevaUser.SSOProvider.OKTA,
            signup = false,
            onPremiumAvailable = onPremiumAvailable,
            onPremiumUnavailable = onPremiumUnavailable
        )
        Spacer(Modifier.height(18.dp))
        LoginButton(
            activityToReturnTo = activityToReturnTo,
            screenToReturnTo = screenToReturnTo,
            provider = NeevaUser.SSOProvider.MICROSOFT,
            signup = false,
            onPremiumAvailable = onPremiumAvailable,
            onPremiumUnavailable = onPremiumUnavailable
        )
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_GoogleSignIn_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        CreateAccountScreen(
            setDefaultAndroidBrowserManager = mockSettingsControllerImpl
                .getSetDefaultAndroidBrowserManager(),
            onShowOtherSignUpOptions = { },
            navigateToSignIn = { },
            selectedSubscriptionPlanTag = null,
            onBack = { },
        )
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_GoogleSignIn_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        CreateAccountScreen(
            setDefaultAndroidBrowserManager = mockSettingsControllerImpl
                .getSetDefaultAndroidBrowserManager(),
            onShowOtherSignUpOptions = { },
            navigateToSignIn = { },
            selectedSubscriptionPlanTag = null,
            onBack = { },
        )
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_OtherSignIn_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        CreateAccountScreen(
            setDefaultAndroidBrowserManager = mockSettingsControllerImpl
                .getSetDefaultAndroidBrowserManager(),
            navigateToSignIn = { },
            selectedSubscriptionPlanTag = null,
            onBack = { },
        )
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_OtherSignIn_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        CreateAccountScreen(
            setDefaultAndroidBrowserManager = mockSettingsControllerImpl
                .getSetDefaultAndroidBrowserManager(),
            navigateToSignIn = { },
            selectedSubscriptionPlanTag = null,
            onBack = { },
        )
    }
}
