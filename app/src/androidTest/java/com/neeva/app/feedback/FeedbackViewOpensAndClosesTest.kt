package com.neeva.app.feedback

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
import com.neeva.app.waitForNavDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FeedbackViewOpensAndClosesTest {
    @get:Rule(order = 0)
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 1)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun opensAndClosesSupportSuccessfully() {
        // https://github.com/neevaco/neeva-android/pull/604
        val scenario = androidComposeRule.activityRule.scenario
        val resources = androidComposeRule.activity.resources

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()

        androidComposeRule
            .onNodeWithContentDescription(resources.getString(R.string.toolbar_neeva_menu))
            .performClick()
        androidComposeRule
            .onNodeWithContentDescription(resources.getString(R.string.feedback))
            .performClick()

        // Wait for the Support screen to show up.
        androidComposeRule.waitForNavDestination(AppNavDestination.FEEDBACK)
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.submit_feedback_help_center_title))
            .assertExists()
        androidComposeRule.waitForIdle()

        // Go back to the browser.
        androidComposeRule.runOnUiThread {
            androidComposeRule.activity.onBackPressed()
        }
        androidComposeRule.waitForNavDestination(AppNavDestination.BROWSER)
        androidComposeRule.waitForIdle()

        // Ideally, we would check if the embeddability mode is set back to the original value, but
        // WebLayer doesn't expose that.
    }
}
