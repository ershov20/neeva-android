// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.loadUrlInCurrentTab
import com.neeva.app.longPressOnBrowserView
import com.neeva.app.onBackPressed
import com.neeva.app.selectItemFromContextMenu
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForBrowserState
import com.neeva.app.waitForTitle
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/** Tests long pressing on a link and opening new tabs via the context menu. */
@HiltAndroidTest
class LongPressChildTabBehaviorTest : BaseBrowserTest() {
    private val testUrl = WebpageServingRule.urlFor("big_link_element.html")

    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun createRegularChildTab() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlInCurrentTab(testUrl)
            waitForTitle("Page 1")
            expectBrowserState(isIncognito = false, regularTabCount = 1)

            // Open the link in a new child tab via the context menu.  The test website is just a link
            // that spans the entire page.
            longPressOnBrowserView()
            selectItemFromContextMenu(R.string.menu_open_in_new_tab)
            waitForIdle()

            // Wait for the second tab to be created.
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)
        }
    }

    @Test
    fun createIncognitoChildTab() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlInCurrentTab(testUrl)
            waitForTitle("Page 1")
            expectBrowserState(isIncognito = false, regularTabCount = 1)

            // Open the link in a new child tab via the context menu.  The test website is just a link
            // that spans the entire page.
            longPressOnBrowserView()
            selectItemFromContextMenu(R.string.menu_open_in_new_incognito_tab)
            waitForIdle()

            // Wait until the new incognito tab is created.
            waitForBrowserState(
                isIncognito = true,
                expectedNumIncognitoTabs = 1,
                expectedNumRegularTabs = 1
            )
            waitForTitle("Page 2")
            onNodeWithContentDescription(
                getString(R.string.tracking_protection_incognito_content_description)
            ).assertExists()

            // Make sure we've still only got one regular profile tab open.
            expectBrowserState(
                isIncognito = true,
                incognitoTabCount = 1,
                regularTabCount = 1
            )
        }
    }

    @Test
    fun closingChildTabReturnsToParent() {
        // Load the test webpage up in the existing tab.
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            loadUrlInCurrentTab(testUrl)
            waitForTitle("Page 1")

            // Open the link in a new child tab via the context menu.  The test website is just a link
            // that spans the entire page.
            longPressOnBrowserView()
            selectItemFromContextMenu(R.string.menu_open_in_new_tab)
            waitForIdle()

            // Wait until the new tab is created.
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)
            waitForTitle("Page 2")

            // Hit system back to close the tab.  We should end up back on the parent tab.
            onBackPressed()

            // We should be back on the parent tab.
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 1)
            waitForTitle("Page 1")
        }
    }
}
