// Copyright 2023 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.login

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.neeva.app.welcomeflow.WelcomeFlowActivity

/** Manages navigation between the different screens of the [WelcomeFlowActivity]. */
class WelcomeFlowNavModel(private val navHost: NavHostController) {
    fun showCreateAccountWithGoogle() {
        show(
            WelcomeFlowActivity.Companion.Destinations.CREATE_ACCOUNT_WITH_GOOGLE
        ) {

            // Keep the back stack shallow by popping everything off back to the root when returning
            // to the landing page.
            popUpTo(WelcomeFlowActivity.Companion.Destinations.CREATE_ACCOUNT_WITH_GOOGLE.name)
        }
    }

    fun showCreateAccountWithOther() {
        show(
            WelcomeFlowActivity.Companion.Destinations.CREATE_ACCOUNT_WITH_OTHER
        ) {
            launchSingleTop = true
        }
    }

    fun showSignIn() = show(WelcomeFlowActivity.Companion.Destinations.SIGN_IN)
    fun showPlans() = show(WelcomeFlowActivity.Companion.Destinations.PLANS)
    fun showSetDefaultBrowser() = show(
        WelcomeFlowActivity.Companion.Destinations.SET_DEFAULT_BROWSER
    )

    private fun show(
        destination: WelcomeFlowActivity.Companion.Destinations,
        setOptions: NavOptionsBuilder.() -> Unit = {},
    ) {
        show(destinationName = destination.name, setOptions = setOptions)
    }

    internal fun show(
        destinationName: String,
        setOptions: NavOptionsBuilder.() -> Unit = {},
    ) {
        if (navHost.currentDestination?.route == destinationName) return

        if (isValidDestination(destinationName)) {
            navHost.navigate(destinationName) {
                launchSingleTop = true
                setOptions()
            }
        }
    }

    companion object {
        internal fun isValidDestination(route: String): Boolean {
            return WelcomeFlowActivity.Companion.Destinations.values().any { it.name == route }
        }
    }
}
