// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.rules.TestRule
import timber.log.Timber

val WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(10)

fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.getString(
    stringId: Int
): String {
    return activity.resources.getString(stringId)
}

/** Kick the user to the home screen to minimize our app. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.sendAppToBackground() {
    InstrumentationRegistry.getInstrumentation().context.startActivity(
        createAndroidHomeIntent()
    )
    waitFor {
        activity.lifecycle.currentState == Lifecycle.State.CREATED
    }
}

/**
 * Wait for the given node to disappear.  We have to check for both of these conditions because
 * it seems to be racy.
 */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.waitForNodeToDisappear(
    node: SemanticsNodeInteraction
) {
    waitFor {
        val doesNotExist = try {
            node.assertDoesNotExist()
            true
        } catch (e: AssertionError) {
            false
        }

        val isNotDisplayed = try {
            node.assertIsNotDisplayed()
            true
        } catch (e: AssertionError) {
            false
        }
        doesNotExist || isNotDisplayed
    }
}

/**
 * Waits until the provided [condition] becomes true.
 *
 * We have to poll because the [waitForIdle()] doesn't know how to wait for all the asynchronous
 * Flows that the app is using to push data around and all the asynchronous work that is being done
 * by WebLayer when the browser is doing things.
 */
inline fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.waitFor(
    message: String? = null,
    crossinline condition: (A) -> Boolean
) {
    waitForIdle()
    try {
        waitUntil(WAIT_TIMEOUT) {
            condition(activity)
        }
    } catch (e: ComposeTimeoutException) {
        if (message != null) {
            throw ComposeTimeoutException(message)
        } else {
            throw e
        }
    }
}

/** "Freezes" an instrumentation test so that you can examine the app's state. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.freezeTest() {
    waitUntil(TimeUnit.HOURS.toMillis(1)) { false }
}

/**
 * Waits until the provided [condition] stops throwing assertions.
 *
 * Can't seem to find a function in Compose that just tells you whether something is visible without
 * having to throw an assertion, so work with it by catching the assertion.
 */
inline fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.waitForAssertion(
    message: String? = null,
    crossinline condition: (activity: A) -> Unit
) {
    waitFor(message) {
        assertionToBoolean { condition(activity) }
    }
}

/** Hits the system's back button on the UI thread. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.onBackPressed() {
    runOnUiThread { activity.onBackPressed() }
    waitForIdle()
}

/** If the context menu is displayed, perform a click on the menu item with the given id. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.selectItemFromContextMenu(
    itemStringResId: Int
) {
    clickOnNodeWithText(getString(itemStringResId))
}

/** Wait for a Composable to appear with the given content description, then click on it. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>
.clickOnNodeWithContentDescription(description: String) {
    waitForNodeWithContentDescription(description).assertHasClickAction().performClick()
    waitForIdle()
}

/** Wait for a Composable to appear with the given text, then click on it. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.clickOnNodeWithText(
    text: String
) {
    waitForNodeWithText(text).assertHasClickAction().performClick()
    waitForIdle()
}

/** Wait for a Composable to appear with the given [description], then return it. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>
.waitForNodeWithContentDescription(
    description: String
) = waitForNode(hasContentDescription(description))

/** Wait for a Composable to appear with the given tag, then click on it. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.waitForNodeWithTag(
    tag: String
) = waitForNode(hasTestTag(tag))

/** Wait for a Composable to appear with the given text, then click on it. */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.waitForNodeWithText(
    text: String,
    substring: Boolean = false
) = waitForNode(hasText(text, substring = substring))

fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.waitForNode(
    matcher: SemanticsMatcher
): SemanticsNodeInteraction {
    var node: SemanticsNodeInteraction? = null
    waitForAssertion("Failed waiting for ${matcher.description}") {
        node = onNode(matcher).assertExists()
    }
    return node!!
}

/**
 * Sometimes clicking doesn't actually have an effect, resulting in the whole test failing.  Try
 * sending the click again if the first attempt fails.
 *
 * TODO(dan.alcantara): Figure out how to reliably test that clicks happened.
 */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.flakyClickOnNode(
    matcher: SemanticsMatcher,
    expectedCondition: () -> Boolean
) {
    try {
        waitForNode(matcher).performClick()
        waitFor { expectedCondition() }
    } catch (e: ComposeTimeoutException) {
        Timber.e("Failed to click using '${matcher.description}'.  Trying again.")
        waitForNode(matcher).performClick()
        waitFor("Failed to click using '${matcher.description}'") { expectedCondition() }
    }
}

inline fun assertionToBoolean(assertion: () -> Unit): Boolean {
    return try {
        assertion()
        true
    } catch (e: AssertionError) {
        false
    }
}
