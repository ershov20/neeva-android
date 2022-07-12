package com.neeva.app.browsing

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.WebpageServingRule
import com.neeva.app.getString
import com.neeva.app.openCardGrid
import com.neeva.app.openLazyTab
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeToDisappear
import com.neeva.app.waitForNodeWithContentDescription
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LandscapeToolbarTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    override fun setUp() {
        super.setUp()
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Rotate the screen to switch to single toolbar mode.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            waitForIdle()
            waitForNodeToDisappear(onNodeWithTag("BrowserBottomToolbar"))
        }
    }

    @Test
    fun switchesToolbarSetupOnRotation() {
        androidComposeRule.apply {
            // Make sure that the buttons that were on the bottom toolbar are still accessible.
            waitForNodeWithContentDescription(getString(R.string.toolbar_go_back))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(getString(R.string.share))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(getString(R.string.toolbar_save_to_space))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(getString(R.string.toolbar_tab_switcher))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(getString(R.string.tracking_protection))
                .assertIsDisplayed()
        }
    }

    @Test
    fun switchesIncognitoToolbarSetupOnRotation() {
        val testUrl = WebpageServingRule.urlFor("big_link_element.html")
        androidComposeRule.apply {
            // Open an incognito tab.
            openCardGrid(incognito = true)
            openLazyTab(testUrl)

            // Make sure that the buttons that were on the bottom toolbar are still accessible.
            waitForNodeWithContentDescription(getString(R.string.toolbar_go_back))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(getString(R.string.share))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(getString(R.string.toolbar_save_to_space))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(getString(R.string.toolbar_tab_switcher))
                .assertIsDisplayed()
            waitForNodeWithContentDescription(
                getString(R.string.tracking_protection_incognito_content_description)
            ).assertIsDisplayed()
        }
    }
}
