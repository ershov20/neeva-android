package com.neeva.app.browsing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.WebpageServingRule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.createMainIntent
import com.neeva.app.createNeevaActivityAndroidComposeTestRule
import com.neeva.app.expectTabListState
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
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@SuppressWarnings("deprecation")
@HiltAndroidTest
class TabSwitcherTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createNeevaActivityAndroidComposeTestRule(createMainIntent())

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
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = false, regularTabCount = 3)

            // Close all tabs from the menu.
            openOverflowMenuAndClickItem(R.string.close_all_content_description)

            // Confirm the tab closure.
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectTabListState(isIncognito = false, regularTabCount = 0)
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
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = true, incognitoTabCount = 3, regularTabCount = 1)

            // Close all tabs from the menu.
            openOverflowMenuAndClickItem(R.string.close_all_content_description)

            // Confirm the tab closure.
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectTabListState(isIncognito = true, incognitoTabCount = 0, regularTabCount = 1)
        }
    }

    @Test
    fun closeAllTabs_withSettingEnabled_requiresPrompt() {
        androidComposeRule.apply {
            // Turn the setting on.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithText(getString(R.string.settings_when_closing_all_tabs)).performClick()
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Open a bunch of tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = false, regularTabCount = 3)

            // The dialog should appear and no tabs should be closed.
            openOverflowMenuAndClickItem(R.string.close_all_content_description)
            expectTabListState(isIncognito = false, regularTabCount = 3)

            // Confirm the tab closure.
            waitForNodeWithText(getString(android.R.string.ok)).performClick()
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectTabListState(isIncognito = false, regularTabCount = 0)
        }
    }

    @Test
    fun closeAllIncognitoTabs_withSettingEnabled_requiresPrompt() {
        androidComposeRule.apply {
            // Turn the setting on.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithText(getString(R.string.settings_when_closing_all_tabs)).performClick()
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Open a bunch of incognito tabs.
            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("index.html"))
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = true, incognitoTabCount = 3, regularTabCount = 1)

            // The dialog should appear and no tabs should be closed.
            openOverflowMenuAndClickItem(R.string.close_all_content_description)
            expectTabListState(isIncognito = true, incognitoTabCount = 3, regularTabCount = 1)

            // Confirm the tab closure.
            waitForNodeWithText(getString(android.R.string.ok)).performClick()
            waitForAssertion {
                onAllNodesWithTag("TabCard").assertCountEquals(0)
            }
            expectTabListState(isIncognito = true, incognitoTabCount = 0, regularTabCount = 1)
        }
    }

    @Test
    fun closeAllTabs_withSettingEnabled_keepsTabsOpenOnCancel() {
        androidComposeRule.apply {
            // Turn the setting on.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithText(getString(R.string.settings_when_closing_all_tabs)).performClick()
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Open a bunch of tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = false, regularTabCount = 3)

            // The dialog should appear and no tabs should be closed.
            openOverflowMenuAndClickItem(R.string.close_all_content_description)
            expectTabListState(isIncognito = false, regularTabCount = 3)

            // Cancel the tab closure.  The tabs should stick around.
            waitForNodeWithText(getString(android.R.string.cancel)).performClick()
            waitForIdle()

            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = false, regularTabCount = 3)
        }
    }

    @Test
    fun closeAllTabs_onlyAffectsCurrentBrowser() {
        androidComposeRule.apply {
            val testUrl = WebpageServingRule.urlFor("index.html")

            // Create a new incognito tab.
            openCardGrid(incognito = true)
            openLazyTab(testUrl)

            expectTabListState(isIncognito = true, incognitoTabCount = 1, regularTabCount = 1)

            // Close all regular profile tabs from the menu.
            openCardGrid(incognito = false)
            openOverflowMenuAndClickItem(R.string.close_all_content_description)

            // Confirm the tab closure.
            waitForNodeWithText(getString(R.string.empty_regular_tabs_title)).assertIsDisplayed()
            expectTabListState(isIncognito = false, incognitoTabCount = 1, regularTabCount = 0)

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
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = true, incognitoTabCount = 3, regularTabCount = 1)

            // Switch to the regular profile and then back.  No tabs should close.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)
            expectTabListState(isIncognito = false, incognitoTabCount = 3, regularTabCount = 1)

            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = true, incognitoTabCount = 3, regularTabCount = 1)
        }
    }

    @Test
    fun switchingOutOfIncognito_withSetting_closesTabs() {
        androidComposeRule.apply {
            // Turn the "close all tabs when leaving Incognito" setting on.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithTag("SettingsPaneItems").performScrollToNode(
                hasText(getString(R.string.settings_when_leaving_incognito_mode))
            )
            clickOnNodeWithText(getString(R.string.settings_when_leaving_incognito_mode))
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("index.html"))

            // Open a bunch of Incognito tabs.
            visitMultipleSitesInNewTabs()
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(3)
            expectTabListState(isIncognito = true, incognitoTabCount = 3, regularTabCount = 1)

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
            waitForNodeWithText(getString(R.string.empty_incognito_tabs_title)).assertIsDisplayed()
            expectTabListState(isIncognito = true, incognitoTabCount = 0, regularTabCount = 1)
        }
    }
}
