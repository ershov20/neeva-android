// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.WAIT_TIMEOUT
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.closeActiveTabFromTabGrid
import com.neeva.app.enableCloseAllIncognitoTabsSetting
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.onBackPressed
import com.neeva.app.openCardGrid
import com.neeva.app.openLazyTab
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.sendAppToBackground
import com.neeva.app.switchProfileOnCardGrid
import com.neeva.app.visitMultipleSitesInNewTabs
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForAssertion
import com.neeva.app.waitForBrowserState
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeToDisappear
import com.neeva.app.waitForNodeWithContentDescription
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForTitle
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isNotEqualTo

@HiltAndroidTest
class CardGridBehaviorTest : BaseBrowserTest() {
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

    /** Checks for regressions against https://github.com/neevaco/neeva-android/issues/552 */
    @Test
    fun staysInCardGridWithoutAnyTabs() {
        androidComposeRule.apply {
            // Close all the user's tabs.
            openCardGrid(incognito = false)
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)

            // Confirm that we're looking at an empty regular TabGrid.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 0,
                expectedNumIncognitoTabs = null
            )
            onNodeWithText(getString(R.string.tab_switcher_no_tabs)).assertExists()

            // Open the settings page from the tab switcher.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            // Leave Settings.
            onBackPressed()

            // Confirm that we're still at the TabGrid.
            waitForNavDestination(AppNavDestination.CARD_GRID)
            onNodeWithText(getString(R.string.tab_switcher_no_tabs)).assertExists()
        }
    }

    @Test
    fun hittingBack_whileLastTabIsClosing_exitsApp() {
        androidComposeRule.apply {
            val webLayerModel = activity.webLayerModel

            openCardGrid(incognito = false)

            // Close the only tab.
            closeActiveTabFromTabGrid()

            // We should show the empty state.
            waitForNodeWithText(getString(R.string.tab_switcher_no_tabs)).assertIsDisplayed()

            // Hitting back will kill the app because they can't use go back to
            // AppNavDestination.Browser without a tab to view.
            runOnUiThread { activity.onBackPressed() }
            waitUntil(WAIT_TIMEOUT) { activityRule.scenario.state == Lifecycle.State.DESTROYED }

            // The tab should have been closed on the way out.
            expectThat(webLayerModel.browsersFlow.value.regularBrowserWrapper.orderedTabList.value)
                .isEmpty()
        }
    }

    @Test
    fun snackbarDismissal_closesTab() {
        androidComposeRule.apply {
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)

            // Confirm that there are four open tabs.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 4
            )
            onAllNodesWithTag("TabCard").assertCountEquals(4)

            // Close the active tab.
            val firstActiveTabTitle =
                activity.webLayerModel.currentBrowser.activeTabModel.titleFlow.value
            closeActiveTabFromTabGrid()

            // Confirm that there are only three tabs displayed, but three still four live tabs.
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(3)
            }
            expectBrowserState(
                isIncognito = false,
                incognitoTabCount = 0,
                regularTabCount = 4
            )

            // Confirm that a new tab was selected.
            val secondActiveTabTitle =
                activity.webLayerModel.currentBrowser.activeTabModel.titleFlow.value
            expectThat(secondActiveTabTitle).isNotEqualTo(firstActiveTabTitle)
            waitForNodeWithContentDescription(
                activity.getString(R.string.close_tab, secondActiveTabTitle)
            ).assertIsDisplayed()
            onAllNodesWithTag("TabCard").assertCountEquals(3)

            // Hitting back should send the user back to the browser with the second tab loaded.
            onBackPressed()
            waitForTitle("Page 2")

            // Wait for the snackbar to go away and confirm that it results in the third tab getting
            // closed.
            val closeSnackbarText = activity.getString(R.string.closed_tab, firstActiveTabTitle)
            waitForNodeToDisappear(onNodeWithText(closeSnackbarText))
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 3
            )
        }
    }

    @Test
    fun backgroundingApp_immediatelyClosesTab() {
        androidComposeRule.apply {
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 4)

            // Close the active tab.
            closeActiveTabFromTabGrid()

            // Confirm that there are only three tabs displayed, but three still four live tabs.
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(3)
            }
            expectBrowserState(
                isIncognito = false,
                incognitoTabCount = 0,
                regularTabCount = 4
            )

            // Sending the app to the background should immediately close the tab.
            sendAppToBackground()
            expectBrowserState(
                isIncognito = false,
                incognitoTabCount = 0,
                regularTabCount = 3
            )
        }
    }

    @Test
    fun undoTabClosing() {
        androidComposeRule.apply {
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)

            // Confirm that there are four open tabs.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 4
            )
            onAllNodesWithTag("TabCard").assertCountEquals(4)

            // Close the active tab.
            val firstActiveTabTitle =
                activity.webLayerModel.currentBrowser.activeTabModel.titleFlow.value
            closeActiveTabFromTabGrid()
            onAllNodesWithTag("TabCard").assertCountEquals(3)

            // Confirm that a new tab was selected.
            val secondActiveTabTitle =
                activity.webLayerModel.currentBrowser.activeTabModel.titleFlow.value
            expectThat(secondActiveTabTitle).isNotEqualTo(firstActiveTabTitle)
            waitForNodeWithContentDescription(
                activity.getString(R.string.close_tab, secondActiveTabTitle)
            ).assertIsDisplayed()

            // Undo the tab closure.
            waitForNodeWithText(getString(R.string.undo)).performClick()

            // Confirm that we're back up to four tabs.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 4
            )
            onAllNodesWithTag("TabCard").assertCountEquals(4)
        }
    }

    @Test
    fun undoTabClosing_afterAllTabsClosed_doesNothing() {
        androidComposeRule.apply {
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)

            // Confirm that there are four open tabs.
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 4)
            onAllNodesWithTag("TabCard").assertCountEquals(4)

            // Close the active tab.
            closeActiveTabFromTabGrid()

            // Close all the remaining tabs via the menu.
            openOverflowMenuAndClickItem(R.string.menu_close_all_tabs)
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 0)
            onAllNodesWithTag("TabCard").assertCountEquals(0)

            // Try to undo the tab closure.  It should do nothing.
            waitForNodeWithText(getString(R.string.undo)).performClick()
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 0)
            onAllNodesWithTag("TabCard").assertCountEquals(0)
        }
    }

    @Test
    fun closingIncognitoTabThenSwitchingToRegularProfileImmediatelyRemovesProfile() {
        androidComposeRule.apply {
            enableCloseAllIncognitoTabsSetting()

            // Open an Incognito tab.
            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("big_link_element.html"))
            waitForBrowserState(
                isIncognito = true,
                expectedNumIncognitoTabs = 1,
                expectedNumRegularTabs = 1
            )

            // Close the active Incognito tab.
            openCardGrid(incognito = true)
            closeActiveTabFromTabGrid()

            // Confirm that there are no tabs displayed, but the tab is still alive.
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectBrowserState(
                isIncognito = true,
                incognitoTabCount = 1,
                regularTabCount = 1
            )

            // Switch back to the regular profile.
            switchProfileOnCardGrid(incognito = false)

            // Confirm that the incognito profile is deleted immediately.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = null
            )

            // Hitting undo on the snackbar should do nothing.
            waitForNodeWithText(getString(R.string.undo)).performClick()
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = null
            )
        }
    }
}
