package com.neeva.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.cardgrid.SelectedScreen
import java.util.concurrent.TimeUnit
import org.junit.rules.TestRule
import strikt.api.expectThat
import strikt.assertions.isEqualTo

private const val TAG = "ComposeTestUtils"
val WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(10)

fun createAndroidHomeIntent() = Intent()
    .setAction(Intent.ACTION_MAIN)
    .addCategory(Intent.CATEGORY_HOME)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun createMainIntent() = Intent()
    .setAction(Intent.ACTION_MAIN)
    .addCategory(Intent.CATEGORY_LAUNCHER)
    .setComponent(
        ComponentName(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NeevaActivity::class.java
        )
    )

fun createLazyTabIntent() = Intent()
    .setAction(NeevaActivity.ACTION_NEW_TAB)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    .setComponent(
        ComponentName(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NeevaActivity::class.java
        )
    )

fun createSpacesIntent() = Intent()
    .setAction(NeevaActivity.ACTION_SHOW_SPACES)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    .setComponent(
        ComponentName(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NeevaActivity::class.java
        )
    )

fun createViewIntent(url: String) = Intent()
    .setAction(Intent.ACTION_VIEW)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    .setData(Uri.parse(url))
    .setComponent(
        ComponentName(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NeevaActivity::class.java
        )
    )

fun createNeevaActivityAndroidComposeTestRule(intent: Intent) = AndroidComposeTestRule(
    activityRule = ActivityScenarioRule<NeevaActivity>(intent),
    activityProvider = { rule ->
        var activity: NeevaActivity? = null
        rule.scenario.onActivity { activity = it }
        activity!!
    }
)

/** Tries to wait for when the NeevaActivity can start to be interacted with. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForActivityStartup() {
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

            !firstComposeCompleted.isCompleted -> {
                Log.d(TAG, "Not idle -- First compose not completed")
                false
            }

            else -> true
        }
    }
    registerIdlingResource(
        object : IdlingResource {
            override val isIdleNow get() = activity.isBrowserLoadingIdle()
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

fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.getString(stringId: Int): String {
    return activity.resources.getString(stringId)
}

/** Kick the user to the home screen to minimize our app. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.sendAppToBackground() {
    InstrumentationRegistry.getInstrumentation().context.startActivity(
        createAndroidHomeIntent()
    )
    waitUntil(WAIT_TIMEOUT) {
        activity.lifecycle.currentState == Lifecycle.State.CREATED
    }
}

/**
 * Navigates the user to a new website on the current tab.
 *
 * Assumes that the user is in [AppNavDestination.BROWSER].
 */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.loadUrlInCurrentTab(url: String) {
    expectThat(activity.appNavModel!!.currentDestination.value!!.route)
        .isEqualTo(AppNavDestination.BROWSER.route)

    // Click on the URL bar and then type in the provided URL.
    clickOnNodeWithTag("LocationLabel")
    navigateViaUrlBar(url)
}

/**
 * Opens a lazy tab from the current screen of the Card Grid.
 *
 * Assumes that the user is viewing the regular or incognito TabGrid.
 */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.openLazyTab(url: String) {
    expectThat(activity.appNavModel!!.currentDestination.value!!.route)
        .isEqualTo(AppNavDestination.CARD_GRID.route)

    clickOnNodeWithContentDescription(getString(com.neeva.app.R.string.new_tab_content_description))
    waitForNavDestination(AppNavDestination.BROWSER)
    navigateViaUrlBar(url)
}

/** Enters text into the URL bar and hits enter, assuming it is already visible. */
fun <T : TestRule> AndroidComposeTestRule<T, NeevaActivity>.typeIntoUrlBar(text: String) {
    waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
        .performTextInput(text)
}

/** Enters text into the URL bar and hits enter, assuming it is already visible. */
fun <T : TestRule> AndroidComposeTestRule<T, NeevaActivity>.navigateViaUrlBar(url: String) {
    typeIntoUrlBar(url)

    waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
        .performKeyPress(
            androidx.compose.ui.input.key.KeyEvent(
                NativeKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
        )

    val browserViewIdlingResource = object : IdlingResource {
        override val isIdleNow: Boolean get() {
            val bottomToolbarPlaceholderHeight = activity
                .findViewById<View>(R.id.browser_bottom_toolbar_placeholder)
                ?.layoutParams
                ?.height

            return when {
                // Wait until the URL bar state updates enough for the user to see the browser
                // instead of the Zero Query/suggestions pane.
                activity.webLayerModel.currentBrowser.urlBarModel.stateFlow.value.isEditing -> {
                    Log.d(TAG, "Waiting for URL bar to leave editing mode")
                    false
                }

                // Wait until the bottom toolbar becomes visible again after the keyboard goes away.
                bottomToolbarPlaceholderHeight == null || bottomToolbarPlaceholderHeight == 0 -> {
                    Log.d(TAG, "Waiting for bottom toolbar after keyboard dismissal")
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
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForNavDestination(
    destination: AppNavDestination
) {
    // Because a lot of things are changing under the hood and many recompositions are happening,
    // waitForIdle() ends up being flaky.  To counter this, wait until we know we've navigated to
    // the correct screen by looking at the AppNavModel.
    waitForIdle()
    waitUntil(WAIT_TIMEOUT) {
        activity.appNavModel?.currentDestination?.value?.route == destination.route
    }
}

fun <RULE : TestRule> AndroidComposeTestRule<RULE, NeevaActivity>.openOverflowMenuAndClickItem(
    @StringRes labelId: Int
) {
    clickOnNodeWithContentDescription(activity.resources.getString(R.string.toolbar_neeva_menu))
    clickOnNodeWithText(getString(labelId))
}

/** Open the Card Grid by clicking on the Card Grid button from the bottom toolbar. */
fun <RULE : TestRule> AndroidComposeTestRule<RULE, NeevaActivity>.openCardGrid(
    incognito: Boolean,
    expectedSubscreen: SelectedScreen? = null
) {
    waitForIdle()

    when (activity.appNavModel?.currentDestination?.value?.route) {
        AppNavDestination.BROWSER.route -> {
            // Wait for the card grid button to be visible, then click it.
            val cardGridButtonDescription = getString(R.string.toolbar_tab_switcher)
            clickOnNodeWithContentDescription(cardGridButtonDescription)
            waitForNavDestination(AppNavDestination.CARD_GRID)
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

    // Click on the correct tab switcher button.
    val selectedScreen = activity.cardsPaneModel!!.selectedScreen.value
    if (incognito && selectedScreen != SelectedScreen.INCOGNITO_TABS) {
        clickOnNodeWithContentDescription(getString(R.string.incognito))
    } else if (!incognito && selectedScreen != SelectedScreen.REGULAR_TABS) {
        clickOnNodeWithContentDescription(getString(R.string.tabs))
    }

    // Wait for mode switch to kick in.
    waitForIdle()
    waitUntil(WAIT_TIMEOUT) {
        val webLayerModel = activity.webLayerModel
        val browsers = webLayerModel.browsersFlow.value
        browsers.isCurrentlyIncognito == incognito
    }
}

/** Waits for the user to be on a particular sub-screen of the CardGrid. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForCardGridScreen(
    expectedSubscreen: SelectedScreen
) {
    waitUntil(WAIT_TIMEOUT) {
        activity.appNavModel?.currentDestination?.value?.route == AppNavDestination.CARD_GRID.route
    }
    waitUntil(WAIT_TIMEOUT) {
        activity.cardsPaneModel?.selectedScreen?.value == expectedSubscreen
    }
}

/** Waits for the user to be in the correct profile and with the correct number of tabs. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForBrowserState(
    isIncognito: Boolean,
    expectedNumRegularTabs: Int,
    expectedNumIncognitoTabs: Int?
) {
    waitUntil(WAIT_TIMEOUT) {
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

/** Waits until the provided [condition] becomes true. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitFor(
    condition: (activity: NeevaActivity) -> Boolean
) {
    waitUntil(WAIT_TIMEOUT) {
        condition(activity)
    }
}

/**
 * Waits until the provided [condition] stops throwing assertions.
 *
 * Can't seem to find a function in Compose that just tells you whether something is visible without
 * having to throw an assertion, so work with it by catching the assertion.
 */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForAssertion(
    condition: (activity: NeevaActivity) -> Unit
) {
    waitFor {
        try {
            condition(activity)
            true
        } catch (e: java.lang.AssertionError) {
            false
        }
    }
}

/** Hits the system's back button on the UI thread. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.onBackPressed() {
    runOnUiThread { activity.onBackPressed() }
    waitForIdle()
}

/** If the context menu is displayed, perform a click on the menu item with the given id. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.selectItemFromContextMenu(
    itemStringResId: Int
) {
    clickOnNodeWithText(getString(itemStringResId))
}

/** Wait for a Composable to appear with the given content description, then click on it. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.clickOnNodeWithContentDescription(
    description: String
) {
    waitForNodeWithContentDescription(description).performClick()
    waitForIdle()
}

/** Wait for a Composable to appear with the given text, then click on it. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.clickOnNodeWithText(text: String) {
    waitForNodeWithText(text).performClick()
    waitForIdle()
}

/** Wait for a Composable to appear with the given tag, then click on it. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.clickOnNodeWithTag(tag: String) {
    waitForNodeWithTag(tag).performClick()
    waitForIdle()
}

/** Wait for a Composable to appear with the given [description], then return it. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForNodeWithContentDescription(
    description: String
): SemanticsNodeInteraction {
    waitUntil(WAIT_TIMEOUT) {
        onAllNodesWithContentDescription(description)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    return onNodeWithContentDescription(description)
}

/** Wait for a Composable to appear with the given tag, then click on it. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForNodeWithTag(
    tag: String
): SemanticsNodeInteraction {
    waitUntil(WAIT_TIMEOUT) {
        onAllNodesWithTag(tag)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    return onNodeWithTag(tag)
}

/** Wait for a Composable to appear with the given tag, then click on it. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForNodeWithText(
    text: String
): SemanticsNodeInteraction {
    waitUntil(WAIT_TIMEOUT) {
        onAllNodesWithText(text)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    return onNodeWithText(text)
}
