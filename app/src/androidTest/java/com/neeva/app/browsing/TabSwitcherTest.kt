// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.enableCloseAllIncognitoTabsSetting
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.onBackPressed
import com.neeva.app.openCardGrid
import com.neeva.app.openLazyTab
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.visitMultipleSitesInNewTabs
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForAssertion
import com.neeva.app.waitForBrowserState
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeWithText
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@SuppressWarnings("deprecation")
@HiltAndroidTest
class TabSwitcherTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    override fun setUp() {
        super.setUp()
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
        }
    }

    @Test
    fun closeAllTabs_withoutSetting_justClosesAll() {
        androidComposeRule.apply {
            // Open a bunch of tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = false, regularTabCount = 4)

            // Close all tabs from the menu.
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)

            // Confirm the tab closure.
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectBrowserState(isIncognito = false, regularTabCount = 0)
        }
    }

    @Test
    fun closeAllIncognitoTabs_withoutSetting_justClosesAll() {
        androidComposeRule.apply {
            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("index.html"))

            // Open a bunch of Incognito tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = true, incognitoTabCount = 4, regularTabCount = 1)

            // Close all tabs from the menu.
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)

            // Confirm the tab closure.
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectBrowserState(isIncognito = true, incognitoTabCount = 0, regularTabCount = 1)
        }
    }

    @Test
    fun closeAllTabs_withSettingEnabled_requiresPrompt() {
        androidComposeRule.apply {
            // Turn the setting on.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithText(getString(R.string.settings_confirm_close_all_tabs_body))
                .performClick()
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Open a bunch of tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = false, regularTabCount = 4)

            // The dialog should appear and no tabs should be closed.
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)
            expectBrowserState(isIncognito = false, regularTabCount = 4)

            // Confirm the tab closure.
            waitForNodeWithText(getString(android.R.string.ok)).performClick()
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectBrowserState(isIncognito = false, regularTabCount = 0)
        }
    }

    @Test
    fun closeAllIncognitoTabs_withSettingEnabled_requiresPrompt() {
        androidComposeRule.apply {
            // Turn the setting on.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithText(getString(R.string.settings_confirm_close_all_tabs_body))
                .performClick()
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Open a bunch of incognito tabs.
            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("index.html"))
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = true, incognitoTabCount = 4, regularTabCount = 1)

            // The dialog should appear and no tabs should be closed.
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)
            expectBrowserState(isIncognito = true, incognitoTabCount = 4, regularTabCount = 1)

            // Confirm the tab closure.
            waitForNodeWithText(getString(android.R.string.ok)).performClick()
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectBrowserState(isIncognito = true, incognitoTabCount = 0, regularTabCount = 1)
        }
    }

    @Test
    fun closeAllTabs_withSettingEnabled_keepsTabsOpenOnCancel() {
        androidComposeRule.apply {
            // Turn the setting on.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithText(getString(R.string.settings_confirm_close_all_tabs_body))
                .performClick()
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Open a bunch of tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = false, regularTabCount = 4)

            // The dialog should appear and no tabs should be closed.
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)
            expectBrowserState(isIncognito = false, regularTabCount = 4)

            // Cancel the tab closure.  The tabs should stick around.
            waitForNodeWithText(getString(android.R.string.cancel)).performClick()
            waitForIdle()

            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = false, regularTabCount = 4)
        }
    }

    @Test
    fun closeAllTabs_onlyAffectsCurrentBrowser() {
        androidComposeRule.apply {
            val testUrl = WebpageServingRule.urlFor("index.html")

            // Create a new incognito tab.
            openCardGrid(incognito = true)
            openLazyTab(testUrl)

            expectBrowserState(isIncognito = true, incognitoTabCount = 1, regularTabCount = 1)

            // Close all regular profile tabs from the menu.
            openCardGrid(incognito = false)
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)

            // Confirm the tab closure.
            waitForNodeWithText(getString(R.string.tab_switcher_no_tabs)).assertIsDisplayed()
            expectBrowserState(isIncognito = false, incognitoTabCount = 1, regularTabCount = 0)

            // Swap over to incognito and confirm the tab is still there.
            openCardGrid(incognito = true)
            waitForAssertion { onAllNodesWithTag("TabCard").assertCountEquals(1) }
        }
    }

    @Test
    fun switchingOutOfIncognito_withoutSetting_keepsTabs() {
        androidComposeRule.apply {
            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("index.html"))

            // Open a bunch of Incognito tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = true, incognitoTabCount = 4, regularTabCount = 1)

            // Switch to the regular profile and then back.  No tabs should close.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)
            expectBrowserState(isIncognito = false, incognitoTabCount = 4, regularTabCount = 1)

            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = true, incognitoTabCount = 4, regularTabCount = 1)
        }
    }

    @Test
    fun switchingOutOfIncognito_withSetting_closesTabs() {
        androidComposeRule.apply {
            enableCloseAllIncognitoTabsSetting()

            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("index.html"))

            // Open a bunch of Incognito tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(4)
            expectBrowserState(isIncognito = true, incognitoTabCount = 4, regularTabCount = 1)

            // Switch to the regular profile.  All the tabs should close.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)
            waitForBrowserState(
                isIncognito = false,
                expectedNumIncognitoTabs = null,
                expectedNumRegularTabs = 1
            )

            // The Incognito tab grid should be empty.
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(0)
            waitForNodeWithText(getString(R.string.tab_switcher_no_incognito_tabs))
                .assertIsDisplayed()
            expectBrowserState(isIncognito = true, incognitoTabCount = 0, regularTabCount = 1)
        }
    }
}
