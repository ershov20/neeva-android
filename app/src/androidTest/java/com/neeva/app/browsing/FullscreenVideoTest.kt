package com.neeva.app.browsing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.WebpageServingRule
import com.neeva.app.expectTabListState
import com.neeva.app.loadUrlInCurrentTab
import com.neeva.app.onBackPressed
import com.neeva.app.tapOnBrowserView
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForAssertion
import com.neeva.app.waitForTitle
import com.neeva.app.waitForUrl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class FullscreenVideoTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun backExitsFullscreen() {
        val testUrl = WebpageServingRule.urlFor("video.html")

        val scenario = androidComposeRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.apply {
            waitForActivityStartup()
            expectTabListState(isIncognito = false, regularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlInCurrentTab(testUrl)
            waitForTitle("Fullscreen video test")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectTabListState(isIncognito = false, regularTabCount = 1)

            // Click on the page, which should will make the video play in fullscreen.
            tapOnBrowserView()
            waitForIdle()

            waitForAssertion {
                onNodeWithTag("LocationLabel").assertIsNotDisplayed()
            }
            expectThat(activity.webLayerModel.currentBrowser.isFullscreen()).isTrue()

            // After hitting back, you should still be on the same page, but not in fullscreen.
            onBackPressed()
            waitForUrl(testUrl)
            waitForTitle("Fullscreen video test")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectTabListState(isIncognito = false, regularTabCount = 1)

            waitForAssertion {
                onNodeWithTag("LocationLabel").assertIsDisplayed()
            }
            expectThat(activity.webLayerModel.currentBrowser.isFullscreen()).isFalse()
        }
    }
}
