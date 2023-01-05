// Copyright 2023 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.login

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.neeva.app.firstrun.widgets.texts.ToggleSignUpText
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.WelcomeFlowActivity
import com.neeva.app.welcomeflow.WelcomeFlowButtonContainer
import com.neeva.app.welcomeflow.WelcomeFlowContainer

@Composable
fun SignInScreen(
    setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
    navigateToCreateAccount: () -> Unit,
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
                    WelcomeFlowActivity::class.java.name,
                    WelcomeFlowActivity.Companion.Destinations.FINISH_WELCOME_FLOW.name
                )
            } else {
                ActivityReturnParams(
                    NeevaActivity::class.java.name,
                    AppNavDestination.SETTINGS.name
                )
            }
        } else {
            ActivityReturnParams(
                WelcomeFlowActivity::class.java.name,
                WelcomeFlowActivity.Companion.Destinations.SET_DEFAULT_BROWSER.name
            )
        }

    WelcomeFlowContainer(
        onBack = onBack,
        headerText = stringResource(id = R.string.welcomeflow_sign_in_to_neeva)
    ) {
        Column(modifier = it) {
            Spacer(Modifier.height(96.dp))
            WelcomeFlowButtonContainer {
                LoginButton(
                    activityToReturnTo = activityReturnParams.activityToReturnTo,
                    screenToReturnTo = activityReturnParams.screenToReturnTo,
                    provider = NeevaUser.SSOProvider.GOOGLE,
                    signup = false,
                    onPremiumAvailable = onPremiumAvailable,
                    onPremiumUnavailable = onPremiumUnavailable
                )
                Spacer(Modifier.height(18.dp))
                LoginButton(
                    activityToReturnTo = activityReturnParams.activityToReturnTo,
                    screenToReturnTo = activityReturnParams.screenToReturnTo,
                    provider = NeevaUser.SSOProvider.OKTA,
                    signup = false,
                    onPremiumAvailable = onPremiumAvailable,
                    onPremiumUnavailable = onPremiumUnavailable
                )
                Spacer(Modifier.height(18.dp))
                LoginButton(
                    activityToReturnTo = activityReturnParams.activityToReturnTo,
                    screenToReturnTo = activityReturnParams.screenToReturnTo,
                    provider = NeevaUser.SSOProvider.MICROSOFT,
                    signup = false,
                    onPremiumAvailable = onPremiumAvailable,
                    onPremiumUnavailable = onPremiumUnavailable
                )
            }
            Spacer(Modifier.height(32.dp))
            ToggleSignUpText(signup = false, onClick = navigateToCreateAccount)
        }
    }
}

@PortraitPreviews
@Composable
fun SignIn_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        SignInScreen(
            setDefaultAndroidBrowserManager = mockSettingsControllerImpl
                .getSetDefaultAndroidBrowserManager(),
            navigateToCreateAccount = { },
            selectedSubscriptionPlanTag = null,
            onBack = { }
        )
    }
}

@PortraitPreviews
@Composable
fun SignIn_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        SignInScreen(
            setDefaultAndroidBrowserManager = mockSettingsControllerImpl
                .getSetDefaultAndroidBrowserManager(),
            navigateToCreateAccount = { },
            selectedSubscriptionPlanTag = null,
            onBack = { }
        )
    }
}
