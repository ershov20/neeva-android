package com.neeva.app.feedback

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.TestNeevaConstantsModule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.expectTabListState
import com.neeva.app.getString
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForUrl
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class FeedbackViewTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun visitHelpCenter() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Navigate to Support.
            openOverflowMenuAndClickItem(R.string.feedback)
            waitForNavDestination(AppNavDestination.FEEDBACK)

            // Click on "Visit our help center".  It should open a new tab to load the Neeva URL.
            // Not super happy with this test because is actively loads the real Neeva website.
            waitForNodeWithText(getString(R.string.submit_feedback_help_center_link)).performClick()
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForUrl(TestNeevaConstantsModule.neevaConstants.appHelpCenterURL)
            expectTabListState(isIncognito = false, regularTabCount = 2)
        }
    }
}
