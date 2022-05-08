package com.neeva.app.browsing

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.waitForActivityStartup
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class IncognitoOrientationChangeTest {
    @get:Rule(order = 0)
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 1)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun doesNotCrashOnOrientationChange() {
        // Checks for regressions against https://github.com/neevaco/neeva-android/issues/452
        val scenario = androidComposeRule.activityRule.scenario
        val resources = androidComposeRule.activity.resources

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()

        // Open the tab switcher.  Because a lot of things are changing under the hood and many
        // recompositions are happening, waitForIdle() ends up being flaky.  Instead, wait until we
        // know we've navigated to the correct screen by looking at the AppNavModel.
        androidComposeRule
            .onNodeWithContentDescription(resources.getString(R.string.toolbar_tab_switcher))
            .performClick()
        androidComposeRule.waitForIdle()
        androidComposeRule.waitUntil(TimeUnit.SECONDS.toMillis(5)) {
            val appNavModel = androidComposeRule.activity.appNavModel
            val currentRoute = appNavModel?.currentDestination?.value?.route
            currentRoute == AppNavDestination.CARD_GRID.route
        }

        // Switch to the Incognito screen.
        androidComposeRule
            .onNodeWithContentDescription(resources.getString(R.string.incognito))
            .performClick()

        // Confirm that we're looking at an empty incognito TabGrid.
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.empty_incognito_tabs_title))
            .assertExists()

        // Rotate the screen, which normally triggers the crash.
        androidComposeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        androidComposeRule.waitForIdle()

        scenario.close()
    }
}
