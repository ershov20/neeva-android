package com.neeva.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.cardgrid.SelectedScreen
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@HiltAndroidTest
class IntentLaunchTest : BaseBrowserTest() {
    // We need to use a deprecated class because we're manually firing different Intents to start
    // our Activity, which ActivityScenarioRule doesn't seem to support (it ends up fully killing
    // the original Activity creating a new one whenever we fire another Intent).
    @Suppress("DEPRECATION")
    private val activityTestRule = androidx.test.rule.ActivityTestRule(
        MainActivity::class.java,
        false,
        false
    )

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = true, skipNeevaScopeTooltip = true)

    @get:Rule
    val multiActivityTestRule = MultiActivityTestRule()

    /**
     * Rule that doesn't try to launch the Activity with the given activityRule.  It must be paired
     * with a [MultiActivityTestRule] to allow the test to find the correct activity after it has
     * launched.
     */
    @get:Rule(order = 10000)
    val androidComposeRule = AndroidComposeTestRule(
        activityRule = TestRule { base, _ -> base },
        activityProvider = { multiActivityTestRule.getNeevaActivity()!! }
    )

    @Test
    fun externalViewIntent_whileInBackground_opensNewRegularTab() {
        val firstIntent = createMainIntent()
        activityTestRule.launchActivity(firstIntent)

        androidComposeRule.apply {
            waitForActivityStartup()

            // Confirm that we see one tab in the TabGrid.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = null
            )

            // Navigate back to the browser.
            onBackPressed()
            androidComposeRule.waitForNavDestination(AppNavDestination.BROWSER)

            // Kick the user to the home screen to minimize our browser.
            sendAppToBackground()

            // Send the user back into the app with an external Intent.
            val secondIntent = createViewIntent(WebpageServingRule.urlFor("?external_intent"))
            InstrumentationRegistry.getInstrumentation().context.startActivity(secondIntent)

            // Confirm that we see two tabs in the tab switcher.
            waitForNavDestination(AppNavDestination.BROWSER)
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(2)
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 2,
                expectedNumIncognitoTabs = null
            )
        }

        activityTestRule.finishActivity()
    }

    @Test
    fun externalViewIntent_whileInIncognito_opensNewRegularTab() {
        activityTestRule.launchActivity(createMainIntent())

        androidComposeRule.apply {
            waitForActivityStartup()

            // Confirm that we see one tab in the TabGrid.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = null
            )

            // Open an incognito tab to force the user into Incognito mode.
            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("?incognito"))

            // Wait for the incognito Browser tab count to be correct.
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForBrowserState(
                isIncognito = true,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = 1
            )

            // Kick the user to the home screen to minimize our browser.
            sendAppToBackground()

            // Send the user back into the app with an external Intent.
            InstrumentationRegistry.getInstrumentation().context
                .startActivity(createViewIntent(WebpageServingRule.urlFor("?external_intent")))

            // Confirm that we see two regular tabs in the tab switcher.
            waitForNavDestination(AppNavDestination.BROWSER)
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(2)
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 2,
                expectedNumIncognitoTabs = 1
            )
        }

        activityTestRule.finishActivity()
    }

    @Test
    fun newTabIntent_fromColdStart_opensLazyTab() {
        activityTestRule.launchActivity(createLazyTabIntent())

        androidComposeRule.apply {
            waitForActivityStartup()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Confirm that the user is in the Browser in the Lazy Tab state.
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertIsFocused()
            waitForNodeWithText(getString(R.string.suggested_sites)).assertIsDisplayed()

            // There should only be one tab until the user hits enter.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = null
            )

            navigateViaUrlBar(WebpageServingRule.urlFor("?lazily_created_tab"))

            // Wait until the other tab registers.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 2,
                expectedNumIncognitoTabs = null
            )
        }

        activityTestRule.finishActivity()
    }

    @Test
    fun newTabIntent_whileInBackground_opensLazyTab() {
        activityTestRule.launchActivity(createMainIntent())

        androidComposeRule.apply {
            waitForActivityStartup()

            // Confirm that we see one tab in the TabGrid.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)

            // Kick the user to the home screen to minimize our browser.
            sendAppToBackground()

            // Send the user back into the app with a lazy tab Intent.
            InstrumentationRegistry.getInstrumentation().context.startActivity(
                createLazyTabIntent()
            )

            // Confirm that the user is in the Browser in the Lazy Tab state.
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertIsFocused()
            waitForNodeWithText(getString(R.string.suggested_sites)).assertIsDisplayed()

            // There should only be one tab until the user hits enter.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = null
            )

            navigateViaUrlBar(WebpageServingRule.urlFor("?lazily_created_tab"))

            // Wait until the other tab registers.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 2,
                expectedNumIncognitoTabs = null
            )
        }

        activityTestRule.finishActivity()
    }

    @Test
    fun newTabIntent_whileIncognitoAndInBackground_opensLazyTab() {
        activityTestRule.launchActivity(createMainIntent())
        androidComposeRule.apply {
            waitForActivityStartup()

            // Confirm that we see one tab in the TabGrid.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)

            // Open an incognito tab to force the user into Incognito mode.
            openCardGrid(incognito = true)
            openLazyTab(WebpageServingRule.urlFor("?incognito"))

            // Confirm that we're currently in incognito with one tab in each browser profile.
            waitForBrowserState(
                isIncognito = true,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = 1
            )

            // Kick the user to the home screen to minimize our browser.
            sendAppToBackground()

            // Send the user back into the app with a lazy tab Intent.
            InstrumentationRegistry.getInstrumentation().context.startActivity(
                createLazyTabIntent()
            )

            // Confirm that the user is in the Browser in the Lazy Tab state.
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertIsFocused()
            waitForNodeWithText(getString(R.string.suggested_sites)).assertIsDisplayed()

            // There should only be one tab until the user hits enter.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 1,
                expectedNumIncognitoTabs = 1
            )

            navigateViaUrlBar(WebpageServingRule.urlFor("?lazily_created_tab"))

            // Confirm we're currently NOT in incognito, with 2 regular tabs and 1 incognito tab.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 2,
                expectedNumIncognitoTabs = 1
            )
        }

        activityTestRule.finishActivity()
    }

    @Test
    fun spacesIntent_fromColdStart_opensSpaces() {
        activityTestRule.launchActivity(createSpacesIntent())

        androidComposeRule.apply {
            waitForActivityStartup()

            // Confirm that the user is in the Spaces CardGrid.
            waitForCardGridScreen(SelectedScreen.SPACES)
            waitForNodeWithContentDescription(getString(R.string.space_create)).assertIsDisplayed()
        }

        activityTestRule.finishActivity()
    }

    @Test
    fun spacesIntent_whileInBackground_opensSpaces() {
        activityTestRule.launchActivity(createMainIntent())

        androidComposeRule.apply {
            waitForActivityStartup()

            // Confirm that we see one tab in the TabGrid.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(1)

            // Navigate back to the browser.
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Kick the user to the home screen to minimize our browser.
            sendAppToBackground()

            // Send the user back into the app with a Spaces Intent.  The user should end up on the
            // Spaces screen of the CardGrid.
            InstrumentationRegistry.getInstrumentation().context.startActivity(createSpacesIntent())

            // Confirm that the user is in the Spaces CardGrid.
            waitForCardGridScreen(SelectedScreen.SPACES)
            waitForNodeWithContentDescription(getString(R.string.space_create)).assertIsDisplayed()
        }

        activityTestRule.finishActivity()
    }
}
