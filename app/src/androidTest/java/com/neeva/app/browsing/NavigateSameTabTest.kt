// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.clearUrlBarByMashingDelete
import com.neeva.app.clickOnBrowserAndWaitForUrlToLoad
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.loadUrlByClickingOnBar
import com.neeva.app.navigateViaUrlBar
import com.neeva.app.onBackPressed
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeWithContentDescription
import com.neeva.app.waitForTitle
import com.neeva.app.waitForUrl
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@HiltAndroidTest
class NavigateSameTabTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun navigateSameTab() {
        val testUrl = WebpageServingRule.urlFor("big_link_element.html")

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlByClickingOnBar(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)

            // Click on the page, which should load a URL in the current tab.
            clickOnBrowserAndWaitForUrlToLoad("$testUrl?page_index=2")
            waitForTitle("Page 2")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)

            // After hitting back, you should be on the previous page and be able to hit forward.
            onBackPressed()
            waitForUrl(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isTrue()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)
        }
    }

    @Test
    fun navigateNewTab() {
        val testUrl = WebpageServingRule.urlFor("big_link_element_target_blank.html")

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlByClickingOnBar(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)

            // Click on the page, which should load a URL in a new tab because it's set the target.
            clickOnBrowserAndWaitForUrlToLoad("$testUrl?page_index=2")
            waitForIdle()

            // Confirm that the URL represents the updated destination.
            waitForTitle("Page 2")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 3)

            // Hitting back should close the tab and send you back to the parent.  Because a new tab
            // was created, you can't re-open the closed tab.
            onBackPressed()
            waitForUrl(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)
        }
    }

    @Test
    fun editCurrentUrlAndNavigateBackAndForth() {
        val testUrl = WebpageServingRule.urlFor("big_link_element_target_blank.html")

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlByClickingOnBar(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)

            // Click on the URL bar to bring up the search bar.  It should be showing the
            // placeholder text.
            clickOnUrlBar()
            onNodeWithContentDescription(getString(R.string.clear)).assertDoesNotExist()

            // Clicking to edit the URL should copy in the current URL.
            clickOnNodeWithText(getString(R.string.edit_current_url))
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertTextEquals(testUrl)

            // Clear the URL and type in a different one.
            val newUrl = WebpageServingRule.urlFor("audio.html")
            clearUrlBarByMashingDelete()
            navigateViaUrlBar(newUrl)
            waitForTitle("Audio controls test")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)

            // After hitting back, you should be on the previous page and be able to hit forward.
            onBackPressed()
            waitForUrl(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isTrue()
            }
            expectBrowserState(isIncognito = false, regularTabCount = 2)
        }
    }
}
