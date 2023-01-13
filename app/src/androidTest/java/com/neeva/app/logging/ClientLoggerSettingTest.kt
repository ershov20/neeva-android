// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.logging

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.LogMutation
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.getString
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.toggleUsageLoggingSetting
import com.neeva.app.waitFor
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse

@HiltAndroidTest
class ClientLoggerSettingTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper

    private lateinit var testAuthenticatedApolloWrapper: TestAuthenticatedApolloWrapper

    override fun setUp() {
        super.setUp()
        testAuthenticatedApolloWrapper =
            authenticatedApolloWrapper as TestAuthenticatedApolloWrapper
    }

    @Test
    fun togglingClientLoggingOff_disablesLogsFromBeingSent() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            toggleUsageLoggingSetting()
            navigateToSignIn()
            waitForIdle()

            expectThat(
                testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations.any {
                    it is LogMutation
                }
            ).isFalse()
        }
    }

    @Test
    fun leavingClientLoggingToggleOn_sendsLogs() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            navigateToSignIn()
            waitForIdle()

            waitFor {
                testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations.any {
                    it is LogMutation
                }
            }
        }
    }

    private fun navigateToSignIn() {
        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            // Clear out any existing logged operations.
            testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations.clear()

            // Navigate to the sign-up screen
            onNodeWithText(getString(R.string.settings_sign_in_to_join_neeva)).performClick()

            // Navigate to a sign-in screen that triggers a client log.
            clickOnNodeWithText(getString(R.string.sign_in), substring = true)
            waitForIdle()
        }
    }
}
