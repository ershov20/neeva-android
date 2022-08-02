package com.neeva.app.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.TestNeevaConstantsModule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.clickOnNodeWithContentDescription
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectBrowserState
import com.neeva.app.getSelectedTabNode
import com.neeva.app.getString
import com.neeva.app.onBackPressed
import com.neeva.app.openCardGrid
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.typeIntoUrlBar
import com.neeva.app.visitMultipleSitesInSameTab
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForAssertion
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeToDisappear
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForTitle
import com.neeva.app.waitForUrl
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse

@HiltAndroidTest
class HistoryUITest : BaseBrowserTest() {
    private val testUrl = WebpageServingRule.urlFor("big_link_element.html")

    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Before
    override fun setUp() {
        super.setUp()
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)
        }
    }

    @Test
    fun deleteItemFromHistory() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()

            // Open up history.
            openOverflowMenuAndClickItem(R.string.history)
            val page1Node = waitForNodeWithText("Page 1").assertIsDisplayed()
            val page2Node = waitForNodeWithText("Page 2").assertIsDisplayed()
            val page3Node = waitForNodeWithText("Page 3").assertIsDisplayed()

            // Delete some items from history.
            clickOnNodeWithContentDescription(
                activity.resources.getString(R.string.history_remove_visit, "Page 1")
            )
            clickOnNodeWithContentDescription(
                activity.resources.getString(R.string.history_remove_visit, "Page 3")
            )

            page2Node.assertIsDisplayed()
            waitForNodeToDisappear(page1Node)
            waitForNodeToDisappear(page3Node)
        }
    }

    @Test
    fun visitUrlFromHistory() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()

            // Open up history.
            openOverflowMenuAndClickItem(R.string.history)
            waitForNodeWithText("Page 1").assertIsDisplayed()
            waitForNodeWithText("Page 2").assertIsDisplayed()
            waitForNodeWithText("Page 3").assertIsDisplayed()

            // Click on one of the items in history.  It should create a new tab for that URL.
            waitForNodeWithText("Page 1").performClick()
            waitForUrl(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)

            openCardGrid(false)

            // The tab should be selected in the card grid.
            getSelectedTabNode(title = "Page 1").assertIsDisplayed()

            waitForAssertion {
                onAllNodesWithTag("TabCard", useUnmergedTree = true)
                    .filterToOne(hasAnyDescendant(hasText("Page 3")))
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun clearAllHistory() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()

            // Open up history.
            openOverflowMenuAndClickItem(R.string.history)
            waitForNodeWithText("Page 1").assertIsDisplayed()
            waitForNodeWithText("Page 2").assertIsDisplayed()
            waitForNodeWithText("Page 3").assertIsDisplayed()

            // Clear the user's history.
            clickOnNodeWithText(getString(R.string.settings_clear_browsing_data))
            waitForNavDestination(AppNavDestination.CLEAR_BROWSING_SETTINGS)
            clickOnNodeWithText(getString(R.string.settings_clear_selected_data_on_device))
            clickOnNodeWithText(getString(R.string.clear_browsing_everything))
            clickOnNodeWithText(getString(R.string.clear_browsing_clear_data))

            // Go back to history.
            onBackPressed()
            waitForNavDestination(AppNavDestination.HISTORY)
            waitForAssertion { onNodeWithText("Page 1").assertDoesNotExist() }
            waitForAssertion { onNodeWithText("Page 2").assertDoesNotExist() }
            waitForAssertion { onNodeWithText("Page 3").assertDoesNotExist() }

            // Go back to the browser.
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)
        }
    }

    @Test
    fun clearAllHistoryRemovesSuggestions() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()

            // Confirm that all the visited sites show up as suggestions when we type into the URL
            // bar.  Because the keyboard is visible, and because SuggestionPane is a LazyColumn, we
            // can't assert that all the nodes are in the Composition and resort to just checking
            // for a few.
            clickOnUrlBar()
            typeIntoUrlBar("Page")
            val suggestionListNode = waitForNodeWithTag("SuggestionList").assertIsDisplayed()
            waitForNodeWithText(getString(R.string.history)).assertIsDisplayed()
            waitForNodeWithText("Page 1").assertExists()
            waitForNodeWithText("Page 2").assertExists()

            // Open up the history UI.
            onBackPressed()
            waitForNodeToDisappear(suggestionListNode)
            openOverflowMenuAndClickItem(R.string.history)
            waitForNavDestination(AppNavDestination.HISTORY)

            // Clear the user's history.
            clickOnNodeWithText(getString(R.string.settings_clear_browsing_data))
            waitForNavDestination(AppNavDestination.CLEAR_BROWSING_SETTINGS)
            clickOnNodeWithText(getString(R.string.settings_clear_selected_data_on_device))
            clickOnNodeWithText(getString(R.string.clear_browsing_everything))
            clickOnNodeWithText(getString(R.string.clear_browsing_clear_data))

            // Go back to the browser.
            onBackPressed()
            waitForNavDestination(AppNavDestination.HISTORY)
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Because history is cleared, there are no suggestions to show the user and they should
            // be stuck on the Zero Query page.
            clickOnUrlBar()
            typeIntoUrlBar("Page")
            onNodeWithTag("SuggestionList").assertDoesNotExist()
            onNodeWithText(getString(R.string.history)).assertDoesNotExist()
            waitForNodeWithText(getString(R.string.suggested_sites)).assertIsDisplayed()
        }
    }

    @Test
    fun manageNeevaMemory() {
        androidComposeRule.apply {
            // Open up the history UI.
            openOverflowMenuAndClickItem(R.string.history)
            waitForNavDestination(AppNavDestination.HISTORY)

            // Open the Clear Browsing Data screen.
            clickOnNodeWithText(getString(R.string.settings_clear_browsing_data))
            waitForNavDestination(AppNavDestination.CLEAR_BROWSING_SETTINGS)

            // Click on "Manage Neeva Memory".  It should open a new tab to load the Neeva URL.
            clickOnNodeWithText(getString(R.string.settings_manage_neeva_memory))
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForUrl(TestNeevaConstantsModule.neevaConstants.appManageMemory)
            expectBrowserState(isIncognito = false, regularTabCount = 2)
        }
    }
}
