// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.neeva.app.BaseBrowserTest
import com.neeva.app.MainActivity
import com.neeva.app.MultiActivityTestRule
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.createMainIntent
import com.neeva.app.getString
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.singletabbrowser.SingleTabActivity
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.waitFor
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeWithText
import com.neeva.app.welcomeflow.WelcomeFlowActivity
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import strikt.api.expectThat
import strikt.assertions.isTrue

@HiltAndroidTest
class WebSignInFlowTest : BaseBrowserTest() {
    @Suppress("DEPRECATION")
    private val activityTestRule = androidx.test.rule.ActivityTestRule(
        MainActivity::class.java,
        false,
        false
    )

    @get:Rule
    val multiActivityTestRule = MultiActivityTestRule()

    @get:Rule(order = 10000)
    val androidComposeRule = AndroidComposeTestRule(
        activityRule = TestRule { base, _ -> base },
        activityProvider = { multiActivityTestRule.getLastForegroundActivity()!! }
    )

    val neevaActivityComposeRule = androidComposeRule
        as AndroidComposeTestRule<TestRule, NeevaActivity>

    @get:Rule
    val sharedPreferencesRule = PresetSharedPreferencesRule(
        useCustomTabsForLogin = true
    )

    @Inject
    lateinit var firstRunModel: FirstRunModel
    @Inject
    lateinit var neevaConstants: NeevaConstants
    @Inject
    lateinit var oktaSignUpHandler: OktaSignUpHandler

    override fun setUp() {
        super.setUp()
        firstRunModel.setFirstRunDone()
        activityTestRule.launchActivity(createMainIntent())
    }

    @Test
    fun signInUsingGoogle() {
        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is NeevaActivity }
        }

        neevaActivityComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
        }

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is WelcomeFlowActivity }

            clickOnNodeWithText(getString(R.string.sign_in), substring = true)

            waitForNodeWithText(getString(R.string.welcomeflow_sign_in_to_neeva))

            clickOnNodeWithText(getString(R.string.sign_in_with_google))

            waitFor { activity is SingleTabActivity }

            val singleTabActivity = activity as SingleTabActivity

            val expectedUri = firstRunModel.getAndroidCallbackUri(
                signup = false,
                provider = NeevaUser.SSOProvider.GOOGLE,
                mktEmailOptOut = false
            )
            expectThat(
                singleTabActivity.intent.data.toString() == expectedUri.toString()
            ).isTrue()
        }
    }

    @Test
    fun signInUsingMicrosoft() {
        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is NeevaActivity }
        }

        neevaActivityComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
        }

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is WelcomeFlowActivity }

            clickOnNodeWithText(getString(R.string.sign_in), substring = true)

            waitForNodeWithText(getString(R.string.welcomeflow_sign_in_to_neeva))

            clickOnNodeWithText(getString(R.string.sign_in_with_microsoft))

            waitFor { activity is SingleTabActivity }

            val singleTabActivity = activity as SingleTabActivity

            val expectedUri = firstRunModel.getAndroidCallbackUri(
                signup = false,
                provider = NeevaUser.SSOProvider.MICROSOFT,
                mktEmailOptOut = false
            )
            expectThat(
                singleTabActivity.intent.data.toString() == expectedUri.toString()
            ).isTrue()
        }
    }

    @Test
    fun signInUsingEmail() {
        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is NeevaActivity }
        }

        neevaActivityComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
        }

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is WelcomeFlowActivity }

            clickOnNodeWithText(getString(R.string.sign_in), substring = true)

            waitForNodeWithText(getString(R.string.welcomeflow_sign_in_to_neeva))

            clickOnNodeWithText(getString(R.string.sign_in_with_email))

            waitFor { activity is SingleTabActivity }

            val singleTabActivity = activity as SingleTabActivity

            val expectedUri = firstRunModel.getAndroidCallbackUri(
                signup = false,
                provider = NeevaUser.SSOProvider.OKTA,
                mktEmailOptOut = false
            )
            expectThat(
                singleTabActivity.intent.data.toString() == expectedUri.toString()
            ).isTrue()
        }
    }

    @Test
    fun signUpUsingGoogle() {
        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is NeevaActivity }
        }

        neevaActivityComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
        }

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is WelcomeFlowActivity }

            clickOnNodeWithText(getString(R.string.sign_up_with_google))

            waitFor { activity is SingleTabActivity }

            val singleTabActivity = activity as SingleTabActivity

            val expectedUri = firstRunModel.getAndroidCallbackUri(
                signup = true,
                provider = NeevaUser.SSOProvider.GOOGLE,
                mktEmailOptOut = false
            )
            expectThat(
                singleTabActivity.intent.data.toString() == expectedUri.toString()
            ).isTrue()
        }
    }
}
