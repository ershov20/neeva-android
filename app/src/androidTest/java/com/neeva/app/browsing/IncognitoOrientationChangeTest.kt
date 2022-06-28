package com.neeva.app.browsing

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.getString
import com.neeva.app.openCardGrid
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncognitoOrientationChangeTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun doesNotCrashOnOrientationChange() {
        // Checks for regressions against https://github.com/neevaco/neeva-android/issues/452
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Open the incognito tab switcher.
            openCardGrid(incognito = true)

            // Confirm that we're looking at an empty incognito TabGrid.
            waitForNodeWithText(getString(R.string.empty_incognito_tabs_title)).assertExists()

            // Rotate the screen, which normally triggers the crash.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            waitForIdle()
        }
    }
}
