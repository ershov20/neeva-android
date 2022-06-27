package com.neeva.app

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.rules.TestRule
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

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
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.tapOnBrowserView() {
    waitForNodeWithTag("WebLayerContainer").performClick()
}

fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.expectTabListState(
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

fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForTabListState(
    isIncognito: Boolean,
    expectedIncognitoTabCount: Int = 0,
    expectedRegularTabCount: Int = 0
) {
    waitFor {
        val browsers = it.webLayerModel.browsersFlow.value
        val incognitoTabCount =
            (browsers.incognitoBrowserWrapper?.orderedTabList?.value ?: emptyList()).size
        val regularTabCount = browsers.regularBrowserWrapper.orderedTabList.value.size

        return@waitFor when {
            it.webLayerModel.currentBrowser.isIncognito != isIncognito -> false
            incognitoTabCount != expectedIncognitoTabCount -> false
            regularTabCount != expectedRegularTabCount -> false
            else -> true
        }
    }
}

/** Waits for the current tab to show that it has started loading the given URL. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForUrl(url: String) {
    waitFor { it.webLayerModel.currentBrowser.activeTabModel.urlFlow.value.toString() == url }
}

/** Waits for the current tab to show that it is displaying the correct title. */
fun <R : TestRule> AndroidComposeTestRule<R, NeevaActivity>.waitForTitle(title: String) {
    waitFor { it.webLayerModel.currentBrowser.activeTabModel.titleFlow.value == title }
}
