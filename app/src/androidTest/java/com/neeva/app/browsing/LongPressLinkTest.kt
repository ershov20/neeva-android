package com.neeva.app.browsing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.loadUrlInCurrentTab
import com.neeva.app.longPressOnBrowserView
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForTabListState
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LongPressLinkTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun longPressImageLink() {
        val imageLinkUrl = WebpageServingRule.urlFor("image_link_element.html")
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

            waitForActivityStartup()
            waitForTabListState(isIncognito = false, expectedRegularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlInCurrentTab(imageLinkUrl)

            // Long press on the context menu and wait for certain controls to show up.
            longPressOnBrowserView()
            waitForNodeWithText("$imageLinkUrl?page_index=2").assertExists()
            waitForNodeWithText("Image alt title").assertExists()
            onNodeWithTag("MenuHeaderImage").assertExists()
        }
    }

    @Test
    fun longPressNonImageLink() {
        val linkUrl = WebpageServingRule.urlFor("big_link_element.html")
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

            waitForActivityStartup()
            waitForTabListState(isIncognito = false, expectedRegularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlInCurrentTab(linkUrl)

            // Long press on the context menu and wait for certain controls to show up.
            longPressOnBrowserView()
            waitForNodeWithText("$linkUrl?page_index=2").assertExists()
            onNodeWithTag("MenuHeaderLabel").assertDoesNotExist()
            onNodeWithTag("MenuHeaderImage").assertDoesNotExist()
        }
    }
}
