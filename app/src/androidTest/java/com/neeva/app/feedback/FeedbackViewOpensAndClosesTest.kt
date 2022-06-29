package com.neeva.app.feedback

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.openCardGrid
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class FeedbackViewOpensAndClosesTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun opensAndClosesSupportSuccessfully_fromBrowser() {
        // https://github.com/neevaco/neeva-android/pull/604
        val scenario = androidComposeRule.activityRule.scenario

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()

        navigateToSupportAndBack()

        androidComposeRule.waitForNavDestination(AppNavDestination.BROWSER)
    }

    @Test
    fun opensAndClosesSupportSuccessfully_fromCardGrid() {
        // https://github.com/neevaco/neeva-android/issues/639
        val scenario = androidComposeRule.activityRule.scenario

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()

        // Go to the CardGrid before trying to go to Support.
        androidComposeRule.openCardGrid(incognito = false)

        navigateToSupportAndBack()

        androidComposeRule.waitForNavDestination(AppNavDestination.CARD_GRID)
    }

    private fun navigateToSupportAndBack() {
        val resources = androidComposeRule.activity.resources

        androidComposeRule.openOverflowMenuAndClickItem(R.string.feedback)

        // Wait for the Support screen to show up.
        androidComposeRule.waitForNavDestination(AppNavDestination.FEEDBACK)
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.submit_feedback_help_center_title))
            .assertExists()
        androidComposeRule.waitForIdle()

        // Go back to the previous screen.
        androidComposeRule.runOnUiThread {
            androidComposeRule.activity.onBackPressed()
        }

        // Ideally, we would check if the embeddability mode is set back to the original value, but
        // WebLayer doesn't expose that.
    }
}
