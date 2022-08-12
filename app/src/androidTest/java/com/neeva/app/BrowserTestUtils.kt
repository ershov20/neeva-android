package com.neeva.app

import android.content.pm.ActivityInfo
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.cardgrid.SelectedScreen
import com.neeva.testcommon.WebpageServingRule
import org.junit.rules.TestRule
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

private const val TAG = "BrowserTestUtils"

/**
 * Perform a long press on the center of the Browser.
 *
 * WebLayer has no affordance for clicking links on a website, and neither Compose nor Espresso
 * can find links that are displayed by WebLayer.  Chromium works around this by creating a special
 * webpage that displays a big link and clicking on the middle of the website to ensure that it can
 * trigger the correct link:
 * https://source.chromium.org/chromium/chromium/src/+/main:weblayer/browser/android/javatests/src/org/chromium/weblayer/test/EventUtils.java;drc=1946212ac0100668f14eb9e2843bdd846e510a1e;l=14
 *
 * We can't use Compose's long click gesture because it just registers as a single click, for some
 * reason.
 */
/** Long presses on the center of the View containing the WebLayer's Fragment. */
fun longPressOnBrowserView() {
    onView(withId(R.id.weblayer_fragment_view_container)).perform(
        GeneralClickAction(
            Tap.LONG,
            GeneralLocation.VISIBLE_CENTER,
            Press.FINGER,
            InputDevice.SOURCE_UNKNOWN,
            MotionEvent.BUTTON_PRIMARY
        )
    )
}

/**
 * Perform a tap on the center of the Browser.
 *
 * Trying to click on the center of the Browser using Espresso does nothing, for some reason.  Doing
 * a click via Compose's performClick DOES, though, and I don't get why.
 */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.tapOnBrowserView(
    expectedCondition: () -> Boolean
) {
    flakyClickOnNode(hasTestTag("WebLayerContainer")) {
        expectedCondition()
    }
}

fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.clickOnBrowserAndWaitForUrlToLoad(
    url: String
) {
    tapOnBrowserView {
        activity.webLayerModel.currentBrowser.activeTabModel.urlFlow.value.toString() == url
    }
}

fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.expectBrowserState(
    isIncognito: Boolean,
    incognitoTabCount: Int = 0,
    regularTabCount: Int = 0
) {
    activity.run {
        expectThat(webLayerModel.currentBrowser.isIncognito).isEqualTo(isIncognito)

        val browsers = webLayerModel.browsersFlow.value
        expectThat(browsers.incognitoBrowserWrapper?.orderedTabList?.value ?: emptyList())
            .hasSize(incognitoTabCount)
        expectThat(browsers.regularBrowserWrapper.orderedTabList.value)
            .hasSize(regularTabCount)
    }
}

/** Waits for the user to be in the correct profile and with the correct number of tabs. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.waitForBrowserState(
    isIncognito: Boolean,
    expectedNumRegularTabs: Int = 0,
    expectedNumIncognitoTabs: Int? = null
) {
    waitFor {
        val browsers = activity.webLayerModel.browsersFlow.value
        val numRegularTabs = browsers.regularBrowserWrapper.orderedTabList.value.size
        val numIncognitoTabs = browsers.incognitoBrowserWrapper?.orderedTabList?.value?.size
        when {
            numRegularTabs != expectedNumRegularTabs -> false
            numIncognitoTabs != expectedNumIncognitoTabs -> false
            browsers.isCurrentlyIncognito != isIncognito -> false
            else -> true
        }
    }
}

/** Waits for the current tab to show that it has started loading the given URL. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.waitForUrl(url: String) {
    waitForIdle()
    waitFor { it.webLayerModel.currentBrowser.activeTabModel.urlFlow.value.toString() == url }
}

/** Waits for the current tab to show that it is displaying the correct title. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.waitForTitle(title: String) {
    waitForIdle()
    waitFor { it.webLayerModel.currentBrowser.activeTabModel.titleFlow.value == title }
}

/** Loads up a page that has a big clickable link that just navigates in the same tab. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.visitMultipleSitesInSameTab() {
    val testUrl = WebpageServingRule.urlFor("big_link_element.html")

    // Load the test webpage up in the existing tab.
    loadUrlInCurrentTab(testUrl)
    waitForTitle("Page 1")

    // Navigate a couple of times so that we can add entries into history.
    clickOnBrowserAndWaitForUrlToLoad("$testUrl?page_index=2")
    waitForTitle("Page 2")

    clickOnBrowserAndWaitForUrlToLoad("$testUrl?page_index=3")
    waitForTitle("Page 3")

    activity.webLayerModel.currentBrowser.activeTabModel.apply {
        expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
        expectThat(navigationInfoFlow.value.canGoForward).isFalse()
    }
    expectBrowserState(isIncognito = false, regularTabCount = 1)
}

/** Loads up a page that has a big clickable link that just navigates in the same tab. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.visitMultipleSitesInNewTabs() {
    val testUrl = WebpageServingRule.urlFor("big_link_element_target_blank.html")

    // Load the test webpage up in the existing tab.
    loadUrlInCurrentTab(testUrl)
    waitForTitle("Page 1")

    // Navigate a couple of times so that we can add entries into history.
    clickOnBrowserAndWaitForUrlToLoad("$testUrl?page_index=2")
    waitForTitle("Page 2")

    clickOnBrowserAndWaitForUrlToLoad("$testUrl?page_index=3")
    waitForTitle("Page 3")

    activity.webLayerModel.currentBrowser.activeTabModel.apply {
        // Can go back because hitting back will close the tab and send you back to the parent.
        expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
        expectThat(navigationInfoFlow.value.canGoForward).isFalse()
    }
}

/** Returns the node representing the tab with the given [title]. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.getSelectedTabNode(
    title: String
): SemanticsNodeInteraction {
    var node: SemanticsNodeInteraction? = null
    waitForAssertion {
        // Use an unmerged tree to ensure that we can find the "SelectedTabCard" tag.
        // If we don't use an unmerged tree, then the containers all get collapsed and only the
        // ancestor's "TabCard" tag is kept in the tree.
        node = onAllNodesWithTag("TabCard", useUnmergedTree = true)
            .filterToOne(
                hasAnyDescendant(hasTestTag("SelectedTabCard"))
                    .and(hasAnyDescendant(hasText(title)))
            )
    }

    return node!!
}

/** Turn the "close all tabs when leaving Incognito" setting on. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.enableCloseAllIncognitoTabsSetting() {
    openOverflowMenuAndClickItem(R.string.settings)
    waitForNavDestination(AppNavDestination.SETTINGS)
    waitForNodeWithTag("SettingsPaneItems").performScrollToNode(
        hasText(getString(R.string.settings_close_incognito_when_switching_body))
    )
    clickOnNodeWithText(getString(R.string.settings_close_incognito_when_switching_body))
    onBackPressed()
    waitForNavDestination(AppNavDestination.BROWSER)
}

/** Toggle the setting for app usage logging. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.toggleUsageLoggingSetting() {
    openOverflowMenuAndClickItem(R.string.settings)
    waitForNavDestination(AppNavDestination.SETTINGS)
    waitForNodeWithTag("SettingsPaneItems").performScrollToNode(
        hasText(getString(R.string.logging_consent_toggle_title))
    )
    clickOnNodeWithText(getString(R.string.logging_consent_toggle_title))
    onBackPressed()
    waitForNavDestination(AppNavDestination.BROWSER)
}

/** Toggle the setting for advanced tab management. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.toggleAdvancedTabManagement() {
    openOverflowMenuAndClickItem(R.string.settings)
    waitForNavDestination(AppNavDestination.SETTINGS)

    // Activate the setting.
    waitForNodeWithTag("SettingsPaneItems")
        .performScrollToNode(hasText(getString(R.string.settings_automated_tab_management)))
    waitForNode(hasText(getString(R.string.settings_automated_tab_management))).performClick()

    // Go back to the browser screen.
    onBackPressed()
}

fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.closeActiveTabFromTabGrid() {
    val activeTabTitle =
        activity.webLayerModel.currentBrowser.activeTabModel.titleFlow.value
    getSelectedTabNode(activeTabTitle).assertIsDisplayed()
    waitForNodeWithContentDescription(activity.getString(R.string.close_tab, activeTabTitle))
        .performClick()

    // Confirm that the TabCard representing the tab disappears.
    waitForNodeToDisappear(onNode(hasText(activeTabTitle)))

    // Confirm that the snackbar shows up.
    val closeSnackbarText = activity.getString(R.string.closed_tab, activeTabTitle)
    waitForNodeWithText(closeSnackbarText).assertIsDisplayed()
}

fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.clickOnUrlBar() {
    flakyClickOnNode(hasTestTag("LocationLabel")) {
        assertionToBoolean {
            waitForNode(hasTestTag("AutocompleteTextField")).assertIsDisplayed()
        }
    }
    waitForNodeWithContentDescription(getString(com.neeva.app.R.string.url_bar_placeholder))
        .assertTextEquals(getString(com.neeva.app.R.string.url_bar_placeholder))
}

/**
 * Navigates the user to a new website on the current tab.
 *
 * Assumes that the user is in [AppNavDestination.BROWSER].
 */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.loadUrlInCurrentTab(url: String) {
    expectThat(activity.appNavModel!!.currentDestination.value!!.route)
        .isEqualTo(AppNavDestination.BROWSER.route)

    // Click on the URL bar and then type in the provided URL.
    clickOnUrlBar()
    navigateViaUrlBar(url)
}

/**
 * Opens a lazy tab from the current screen of the Card Grid.
 *
 * Assumes that the user is viewing the regular or incognito TabGrid.
 */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.openLazyTab(url: String) {
    expectThat(activity.appNavModel!!.currentDestination.value!!.route)
        .isEqualTo(AppNavDestination.CARD_GRID.route)

    clickOnNodeWithContentDescription(getString(R.string.create_new_tab_a11y))
    waitForNavDestination(AppNavDestination.BROWSER)
    navigateViaUrlBar(url)
}

/** Clears text from the URL bar, assuming it is already visible and has text in it. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.clearUrlBar() {
    flakyClickOnNode(hasContentDescription(getString(com.neeva.app.R.string.clear))) {
        activity.webLayerModel.currentBrowser.urlBarModel.stateFlow.value.userTypedInput.isEmpty()
    }
    waitForAssertion {
        onNodeWithTag("AutocompleteTextField")
            .assertTextEquals(getString(com.neeva.app.R.string.url_bar_placeholder))
    }
}

/** Enters text into the URL bar, assuming it is already visible. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.typeIntoUrlBar(text: String) {
    waitForNodeWithTag("AutocompleteTextField").performTextInput(text)

    // Wait for the UrlBarModel to acknowledge that the text has made it through.
    waitFor {
        it.webLayerModel.currentBrowser.urlBarModel.stateFlow.value.userTypedInput == text
    }
}

/** Enters text into the URL bar and hits enter, assuming it is already visible. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.navigateViaUrlBar(url: String) {
    typeIntoUrlBar(url)

    waitFor {
        val actualUrl =
            it.webLayerModel.currentBrowser.urlBarModel.stateFlow.value.uriToLoad.toString()
        if (actualUrl != url) Log.w(TAG, "Not matching yet: $actualUrl != $url")
        actualUrl == url
    }

    waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
        .performKeyPress(
            androidx.compose.ui.input.key.KeyEvent(
                NativeKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
        )

    val browserViewIdlingResource = object : IdlingResource {
        override val isIdleNow: Boolean get() {
            val bottomToolbarHeight = activity
                .findViewById<View>(R.id.browser_bottom_toolbar_placeholder)
                ?.layoutParams
                ?.height
            val isLandscape =
                activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            return when {
                // Wait until the URL bar state updates enough for the user to see the browser
                // instead of the Zero Query/suggestions pane.
                activity.webLayerModel.currentBrowser.urlBarModel.stateFlow.value.isEditing -> {
                    Log.d(TAG, "Waiting for URL bar to leave editing mode")
                    false
                }

                // If we're not in landscape, wait until the bottom toolbar becomes visible again
                // after the keyboard goes away.
                !isLandscape && (bottomToolbarHeight == null || bottomToolbarHeight == 0) -> {
                    Log.d(TAG, "Waiting for bottom toolbar to appear after keyboard dismissal")
                    false
                }

                else -> true
            }
        }
    }

    registerIdlingResource(browserViewIdlingResource)
    waitForIdle()
    unregisterIdlingResource(browserViewIdlingResource)
    waitForUrl(url)
}

/** Wait for the NavController to tell us the user is at a particular [AppNavDestination]. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.waitForNavDestination(
    destination: AppNavDestination
) {
    waitFor("Navigating to $destination") {
        it.appNavModel?.currentDestination?.value?.route == destination.route
    }
}

fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.openOverflowMenuAndClickItem(
    @StringRes labelId: Int
) {
    flakyClickOnNode(hasContentDescription(getString(R.string.toolbar_menu))) {
        assertionToBoolean {
            waitForNode(hasText(getString(labelId)))
        }
    }
    clickOnNodeWithText(getString(labelId))
}

/** Open the Card Grid by clicking on the Card Grid button from the bottom toolbar. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.openCardGrid(
    incognito: Boolean,
    expectedSubscreen: SelectedScreen? = null
) {
    waitForIdle()

    when (activity.appNavModel?.currentDestination?.value?.route) {
        AppNavDestination.BROWSER.route -> {
            // Wait for the card grid button to be visible, then click it.
            flakyClickOnNode(hasContentDescription(getString(R.string.toolbar_tabs_and_spaces))) {
                assertionToBoolean { waitForNavDestination(AppNavDestination.CARD_GRID) }
            }
        }

        AppNavDestination.CARD_GRID.route -> {
            // Already here.
        }

        else -> {
            TODO("Not supported")
        }
    }

    // Check that the CardGrid is in the correct state.
    expectedSubscreen?.let {
        expectThat(activity.cardsPaneModel!!.selectedScreen.value).isEqualTo(expectedSubscreen)
    }

    switchProfileOnCardGrid(incognito)
}

fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.switchProfileOnCardGrid(
    incognito: Boolean
) {
    // Click on the correct tab switcher button.
    val selectedScreen = activity.cardsPaneModel!!.selectedScreen.value
    if (incognito && selectedScreen != SelectedScreen.INCOGNITO_TABS) {
        clickOnNodeWithContentDescription(getString(R.string.view_incognito_tabs))
    } else if (!incognito && selectedScreen != SelectedScreen.REGULAR_TABS) {
        clickOnNodeWithContentDescription(getString(R.string.view_regular_tabs))
    }

    // Wait for mode switch to kick in.
    waitFor {
        val webLayerModel = activity.webLayerModel
        val browsers = webLayerModel.browsersFlow.value
        browsers.isCurrentlyIncognito == incognito
    }
}

/** Waits for the user to be on a particular sub-screen of the CardGrid. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.waitForCardGridScreen(
    expectedSubscreen: SelectedScreen
) {
    waitForNavDestination(AppNavDestination.CARD_GRID)
    waitFor {
        activity.cardsPaneModel?.selectedScreen?.value == expectedSubscreen
    }
}

/** Tries to wait for when the NeevaActivity can start to be interacted with. */
fun <TR : TestRule> AndroidComposeTestRule<TR, NeevaActivity>.waitForActivityStartup() {
    // Permanently register an IdlingResource that waits for the browser to finish loading its
    // current web page.
    fun NeevaActivity.isBrowserLoadingIdle(): Boolean {
        val browsers = webLayerModel.browsersFlow.value
        val loadingProgress = browsers.getCurrentBrowser().activeTabModel.progressFlow.value

        return when {
            // Wait for the current browser to finish loading whatever it's loading.
            !(loadingProgress == 0 || loadingProgress == 100) -> {
                Log.d(TAG, "Not idle -- Load in progress: $loadingProgress")
                false
            }

            else -> true
        }
    }
    registerIdlingResource(
        object : IdlingResource {
            override val isIdleNow get() = activity.isDestroyed || activity.isBrowserLoadingIdle()
        }
    )

    // Temporarily register an IdlingResource that waits for the browser to get to a good spot in
    // initialization.
    fun NeevaActivity.isIdleForTestInitialization(): Boolean {
        val regularBrowserFragment = getWebLayerFragment(isIncognito = false)
        val regularBrowserViewParent = regularBrowserFragment?.view?.parent as? ViewGroup

        return when {
            !isBrowserPreparedFlow.value -> {
                Log.d(TAG, "Not idle -- NeevaActivity has not finished prepareBrowser")
                false
            }

            // The WebLayer Fragment should be attached to something.  We can't check if it's
            // attached to the Compose hierarchy because some tests skip AppNavDestination.BROWSER
            // and never attach the [WebLayerContainer] composable.
            regularBrowserViewParent == null -> {
                Log.d(TAG, "Not idle -- WebLayer Fragment not attached")
                false
            }

            // The app should always have at least one regular tab on a cold start.
            webLayerModel.browsersFlow.value.regularBrowserWrapper.hasNoTabs() -> {
                Log.d(TAG, "Not idle -- No regular profile tabs detected")
                false
            }

            !firstComposeCompleted.isCompleted -> {
                Log.d(TAG, "Not idle -- First compose not completed")
                false
            }

            else -> true
        }
    }

    val testInitializationIdlingResource = object : IdlingResource {
        override val isIdleNow get() = activity.isIdleForTestInitialization()
    }
    registerIdlingResource(testInitializationIdlingResource)
    waitForIdle()
    unregisterIdlingResource(testInitializationIdlingResource)
    Log.d(TAG, "Proceeding with test")
}
