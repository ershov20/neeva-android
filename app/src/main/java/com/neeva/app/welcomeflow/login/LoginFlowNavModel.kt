// Copyright 2023 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.login

import androidx.navigation.NavHostController
import com.neeva.app.welcomeflow.WelcomeFlowActivity

/** Manages navigation between the different screens of the sign-in flow. */
class LoginFlowNavModel(private val navHost: NavHostController) {
    fun navigateToCreateAccountWithGoogle() {
        navHost.navigate(
            WelcomeFlowActivity.Companion.Destinations.CREATE_ACCOUNT_WITH_GOOGLE.name
        ) {
            launchSingleTop = true

            // Keep the back stack shallow by popping everything off back to the root when returning
            // to the landing page.
            popUpTo(WelcomeFlowActivity.Companion.Destinations.CREATE_ACCOUNT_WITH_GOOGLE.name)
        }
    }

    fun navigateToCreateAccountWithOther() {
        navHost.navigate(
            WelcomeFlowActivity.Companion.Destinations.CREATE_ACCOUNT_WITH_OTHER.name
        ) {
            launchSingleTop = true
        }
    }

    fun navigateToSignIn() {
        navHost.navigate(WelcomeFlowActivity.Companion.Destinations.SIGN_IN.name) {
            launchSingleTop = true
        }
    }
}
