// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForUrl
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsURLsTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Inject
    lateinit var neevaConstants: NeevaConstants

    @Before
    override fun setUp() {
        super.setUp()
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)
        }
    }

    private fun clickOnSettingsItem(
        @StringRes labelId: Int,
        expectedUrl: String,
        expectedTabCount: Int
    ) {
        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithTag("SettingsPaneItems")
                .performScrollToNode(hasText(getString(labelId)))
            clickOnNodeWithText(getString(labelId))

            // Should send the user to a new tab for viewing the URL.
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForUrl(expectedUrl)
            expectBrowserState(isIncognito = false, regularTabCount = expectedTabCount)
        }
    }

    @Test
    fun clickLinks() {
        androidComposeRule.apply {
            clickOnSettingsItem(
                labelId = R.string.settings_connected_apps,
                expectedUrl = neevaConstants.appConnectionsURL,
                expectedTabCount = 2
            )
            clickOnSettingsItem(
                labelId = R.string.settings_invite_friends,
                expectedUrl = neevaConstants.appReferralURL,
                expectedTabCount = 3
            )
            clickOnSettingsItem(
                labelId = R.string.settings_privacy_policy,
                expectedUrl = neevaConstants.appPrivacyURL,
                expectedTabCount = 4
            )
            clickOnSettingsItem(
                labelId = R.string.settings_terms,
                expectedUrl = neevaConstants.appTermsURL,
                expectedTabCount = 5
            )
            clickOnSettingsItem(
                labelId = R.string.settings_account_settings,
                expectedUrl = neevaConstants.appSettingsURL,
                expectedTabCount = 6
            )
        }
    }
}
