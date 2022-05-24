package com.neeva.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.cardgrid.SelectedScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SuppressWarnings("deprecation")
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class IntentLaunchTest {
    // We need to use a deprecated class because we're manually firing different Intents to start
    // our Activity, which ActivityScenarioRule doesn't seem to support (it ends up fully killing
    // the original Activity creating a new one whenever we fire another Intent).
    private val activityTestRule = ActivityTestRule(
        NeevaActivity::class.java,
        false,
        false
    )

    @get:Rule(order = 0)
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 1)
    val androidComposeRule = AndroidComposeTestRule(
        activityRule = activityTestRule,
        activityProvider = { it.activity }
    )

    @Test
    fun externalViewIntent_whileInBackground_opensNewRegularTab() {
        val firstIntent = createMainIntent()
        activityTestRule.launchActivity(firstIntent)
        androidComposeRule.waitForActivityStartup()

        // Confirm that we see one tab in the TabGrid.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(1)
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 1,
            expectedNumIncognitoTabs = null
        )

        // Navigate back to the browser.
        androidComposeRule.runOnUiThread {
            androidComposeRule.activity.onBackPressed()
        }
        androidComposeRule.waitForNavDestination(AppNavDestination.BROWSER)

        // Kick the user to the home screen to minimize our browser.
        androidComposeRule.sendAppToBackground()

        // Send the user back into the app with an external Intent.
        val secondIntent = createViewIntent("http://127.0.0.1?external_intent")
        InstrumentationRegistry.getInstrumentation().context.startActivity(secondIntent)

        // Confirm that we see two tabs in the tab switcher.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(2)
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 2,
            expectedNumIncognitoTabs = null
        )

        activityTestRule.finishActivity()
    }

    @Test
    fun externalViewIntent_whileInIncognito_opensNewRegularTab() {
        activityTestRule.launchActivity(createMainIntent())
        androidComposeRule.waitForActivityStartup()

        // Confirm that we see one tab in the TabGrid.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(1)
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 1,
            expectedNumIncognitoTabs = null
        )

        // Open an incognito tab to force the user into Incognito mode.
        androidComposeRule.openCardGrid(incognito = true)
        androidComposeRule.openLazyTab("http://127.0.0.1?incognito")

        // Wait for the incognito Browser tab count to be correct.
        androidComposeRule.waitForNavDestination(AppNavDestination.BROWSER)
        androidComposeRule.waitForBrowserState(
            isIncognito = true,
            expectedNumRegularTabs = 1,
            expectedNumIncognitoTabs = 1
        )

        // Kick the user to the home screen to minimize our browser.
        androidComposeRule.sendAppToBackground()

        // Send the user back into the app with an external Intent.
        InstrumentationRegistry.getInstrumentation().context
            .startActivity(createViewIntent("http://127.0.0.1?external_intent"))

        // Confirm that we see two regular tabs in the tab switcher.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(2)
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 2,
            expectedNumIncognitoTabs = 1
        )

        activityTestRule.finishActivity()
    }

    @Test
    fun newTabIntent_fromColdStart_opensLazyTab() {
        activityTestRule.launchActivity(createLazyTabIntent())
        androidComposeRule.waitForActivityStartup()
        androidComposeRule.waitForNavDestination(AppNavDestination.BROWSER)

        // Confirm that the user is in the Browser in the Lazy Tab state.
        androidComposeRule
            .onNodeWithContentDescription(
                androidComposeRule.getString(R.string.url_bar_placeholder)
            )
            .assertIsFocused()
        androidComposeRule
            .onNodeWithText(androidComposeRule.getString(R.string.suggested_sites))
            .assertIsDisplayed()

        // There should only be one tab until the user hits enter.
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 1,
            expectedNumIncognitoTabs = null
        )

        androidComposeRule.typeIntoUrlBar("http://127.0.0.1?lazily_created_tab")

        // Wait until the other tab registers.
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 2,
            expectedNumIncognitoTabs = null
        )

        activityTestRule.finishActivity()
    }

    @Test
    fun newTabIntent_whileInBackground_opensLazyTab() {
        activityTestRule.launchActivity(createMainIntent())
        androidComposeRule.waitForActivityStartup()

        // Confirm that we see one tab in the TabGrid.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(1)

        // Kick the user to the home screen to minimize our browser.
        androidComposeRule.sendAppToBackground()

        // Send the user back into the app with a lazy tab Intent.
        InstrumentationRegistry.getInstrumentation().context.startActivity(createLazyTabIntent())

        // Confirm that the user is in the Browser in the Lazy Tab state.
        androidComposeRule
            .onNodeWithContentDescription(
                androidComposeRule.getString(R.string.url_bar_placeholder)
            )
            .assertIsFocused()
        androidComposeRule
            .onNodeWithText(androidComposeRule.getString(R.string.suggested_sites))
            .assertIsDisplayed()

        // There should only be one tab until the user hits enter.
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 1,
            expectedNumIncognitoTabs = null
        )

        androidComposeRule.typeIntoUrlBar("http://127.0.0.1?lazily_created_tab")

        // Wait until the other tab registers.
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 2,
            expectedNumIncognitoTabs = null
        )

        activityTestRule.finishActivity()
    }

    @Test
    fun newTabIntent_whileIncognitoAndInBackground_opensLazyTab() {
        activityTestRule.launchActivity(createMainIntent())
        androidComposeRule.waitForActivityStartup()

        // Confirm that we see one tab in the TabGrid.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(1)

        // Open an incognito tab to force the user into Incognito mode.
        androidComposeRule.openCardGrid(incognito = true)
        androidComposeRule.openLazyTab("http://127.0.0.1?incognito")

        // Confirm that we're currently in incognito with one tab in each browser profile.
        androidComposeRule.waitForBrowserState(
            isIncognito = true,
            expectedNumRegularTabs = 1,
            expectedNumIncognitoTabs = 1
        )

        // Kick the user to the home screen to minimize our browser.
        androidComposeRule.sendAppToBackground()

        // Send the user back into the app with a lazy tab Intent.
        InstrumentationRegistry.getInstrumentation().context.startActivity(createLazyTabIntent())

        // Confirm that the user is in the Browser in the Lazy Tab state.
        androidComposeRule
            .onNodeWithContentDescription(
                androidComposeRule.getString(R.string.url_bar_placeholder)
            )
            .assertIsFocused()
        androidComposeRule
            .onNodeWithText(androidComposeRule.getString(R.string.suggested_sites))
            .assertIsDisplayed()

        // There should only be one tab until the user hits enter.
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 1,
            expectedNumIncognitoTabs = 1
        )

        androidComposeRule.typeIntoUrlBar("http://127.0.0.1?lazily_created_tab")

        // Confirm we're currently NOT in incognito, with two regular tabs and one incognito tab.
        androidComposeRule.waitForBrowserState(
            isIncognito = false,
            expectedNumRegularTabs = 2,
            expectedNumIncognitoTabs = 1
        )

        activityTestRule.finishActivity()
    }

    @Test
    fun spacesIntent_fromColdStart_opensSpaces() {
        activityTestRule.launchActivity(createSpacesIntent())
        androidComposeRule.waitForActivityStartup()

        // Confirm that the user is in the Spaces CardGrid.
        androidComposeRule.waitForCardGridScreen(SelectedScreen.SPACES)

        val createContentDescription =
            activityTestRule.activity.resources.getString(R.string.space_create)
        androidComposeRule.waitForIdle()
        androidComposeRule.onNodeWithContentDescription(createContentDescription).assertExists()

        activityTestRule.finishActivity()
    }

    @Test
    fun spacesIntent_whileInBackground_opensSpaces() {
        activityTestRule.launchActivity(createMainIntent())
        androidComposeRule.waitForActivityStartup()

        // Confirm that we see one tab in the TabGrid.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(1)

        // Navigate back to the browser.
        androidComposeRule.runOnUiThread {
            androidComposeRule.activity.onBackPressed()
        }
        androidComposeRule.waitForNavDestination(AppNavDestination.BROWSER)

        // Kick the user to the home screen to minimize our browser.
        androidComposeRule.sendAppToBackground()

        // Send the user back into the app with a Spaces Intent.  The user should end up on the
        // Spaces screen of the CardGrid.
        InstrumentationRegistry.getInstrumentation().context.startActivity(createSpacesIntent())

        // Confirm that the user is in the Spaces CardGrid.
        androidComposeRule.waitForCardGridScreen(SelectedScreen.SPACES)

        val createContentDescription =
            activityTestRule.activity.resources.getString(R.string.space_create)
        androidComposeRule.waitForIdle()
        androidComposeRule.onNodeWithContentDescription(createContentDescription).assertExists()

        activityTestRule.finishActivity()
    }
}
