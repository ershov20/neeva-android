// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.net.Uri
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.clearUrlBarByMashingDelete
import com.neeva.app.clearUrlBarViaClearButton
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.loadUrlByClickingOnBar
import com.neeva.app.navigateViaUrlBar
import com.neeva.app.onBackPressed
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForBrowserState
import com.neeva.app.waitForNode
import com.neeva.app.waitForTitle
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@HiltAndroidTest
class CreateOrSwitchTabTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    override fun setUp() {
        super.setUp()
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)
        }
    }

    @Test
    fun createChildrenThenGoBackwards() {
        androidComposeRule.apply {
            // Load the test webpage up by tapping on the Location bar, which creates a new tab.
            val firstUrl = WebpageServingRule.urlFor("big_link_element.html")
            loadUrlByClickingOnBar(firstUrl)
            waitForTitle("Page 1")

            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)

            val secondUrl = WebpageServingRule.urlFor("audio.html")
            loadUrlByClickingOnBar(secondUrl)
            waitForTitle("Audio controls test")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)

            onBackPressed()
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)
            waitForTitle("Page 1")

            onBackPressed()
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 1)
            waitForTitle("Instrumentation test homepage")
        }
    }

    @Test
    fun findsExistingTab() {
        androidComposeRule.apply {
            // Load the test webpage up by tapping on the Location bar, which creates a new tab.
            val firstUrl = WebpageServingRule.urlFor("big_link_element.html")
            loadUrlByClickingOnBar(firstUrl)
            waitForTitle("Page 1")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)

            // Load another URL that creates another tab.
            val secondUrl = WebpageServingRule.urlFor("audio.html")
            loadUrlByClickingOnBar(secondUrl)
            waitForTitle("Audio controls test")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)
            activity.webLayerModel.currentBrowser.orderedTabList.value.let { tabs ->
                expectThat(tabs.map { it.url }).containsExactly(
                    Uri.parse(WebpageServingRule.urlFor("")),
                    Uri.parse(WebpageServingRule.urlFor("big_link_element.html")),
                    Uri.parse(WebpageServingRule.urlFor("audio.html"))
                )
                expectThat(tabs.map { it.isSelected }).containsExactly(false, false, true)
            }

            // Load a URL that should bring up the existing tab and select it.
            loadUrlByClickingOnBar(firstUrl)
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)
            activity.webLayerModel.currentBrowser.orderedTabList.value.let { tabs ->
                expectThat(tabs.map { it.url }).containsExactly(
                    Uri.parse(WebpageServingRule.urlFor("")),
                    Uri.parse(WebpageServingRule.urlFor("big_link_element.html")),
                    Uri.parse(WebpageServingRule.urlFor("audio.html"))
                )
                expectThat(tabs.map { it.isSelected }).containsExactly(false, true, false)
            }
        }
    }

    @Test
    fun refiningForcesLoadInSameTab() {
        androidComposeRule.apply {
            val firstUrl = WebpageServingRule.urlFor("big_link_element.html")
            loadUrlByClickingOnBar(firstUrl)
            waitForTitle("Page 1")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)

            val secondUrl = WebpageServingRule.urlFor("audio.html")
            loadUrlByClickingOnBar(secondUrl)
            waitForTitle("Audio controls test")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)

            // Edit the URL so that the user ends up loading the URL in the current tab.
            clickOnUrlBar()
            waitForNode(hasText(getString(R.string.edit_current_url))).performClick()

            // Delete everything in the bar.
            clearUrlBarByMashingDelete()

            // Because we're refining, we should load in the same tab.
            val thirdUrl = WebpageServingRule.urlFor("video.html")
            navigateViaUrlBar(thirdUrl)
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)
            activity.webLayerModel.currentBrowser.orderedTabList.value.let { tabs ->
                expectThat(tabs.map { it.url }).containsExactly(
                    Uri.parse(WebpageServingRule.urlFor("")),
                    Uri.parse(WebpageServingRule.urlFor("big_link_element.html")),
                    Uri.parse(WebpageServingRule.urlFor("video.html"))
                )
                expectThat(tabs.map { it.isSelected }).containsExactly(false, false, true)
            }
        }
    }

    @Test
    fun refiningThenCancelingLoadsDifferentTab() {
        androidComposeRule.apply {
            val firstUrl = WebpageServingRule.urlFor("big_link_element.html")
            loadUrlByClickingOnBar(firstUrl)
            waitForTitle("Page 1")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)

            val secondUrl = WebpageServingRule.urlFor("audio.html")
            loadUrlByClickingOnBar(secondUrl)
            waitForTitle("Audio controls test")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)

            // Edit the URL so that the user ends up loading the URL in the current tab.
            clickOnUrlBar()
            waitForNode(hasText(getString(R.string.edit_current_url))).performClick()

            // Clearing the URL re-enables create or switch behavior.
            clearUrlBarViaClearButton()

            // Because we're no longer refining, we should load in a different tab.
            val thirdUrl = WebpageServingRule.urlFor("video.html")
            navigateViaUrlBar(thirdUrl)
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 4)
            activity.webLayerModel.currentBrowser.orderedTabList.value.let { tabs ->
                expectThat(tabs.map { it.url }).containsExactly(
                    Uri.parse(WebpageServingRule.urlFor("")),
                    Uri.parse(WebpageServingRule.urlFor("big_link_element.html")),
                    Uri.parse(WebpageServingRule.urlFor("audio.html")),
                    Uri.parse(WebpageServingRule.urlFor("video.html"))
                )
                expectThat(tabs.map { it.isSelected }).containsExactly(false, false, false, true)
            }
        }
    }

    @Test
    fun zeroQueryClickReselectsExistingTab() {
        androidComposeRule.apply {
            val firstUrl = WebpageServingRule.urlFor("big_link_element.html")
            loadUrlByClickingOnBar(firstUrl)
            waitForTitle("Page 1")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 2)

            val secondUrl = WebpageServingRule.urlFor("audio.html")
            loadUrlByClickingOnBar(secondUrl)
            waitForTitle("Audio controls test")
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)
            activity.webLayerModel.currentBrowser.orderedTabList.value.let { tabs ->
                expectThat(tabs.map { it.url }).containsExactly(
                    Uri.parse(WebpageServingRule.urlFor("")),
                    Uri.parse(WebpageServingRule.urlFor("big_link_element.html")),
                    Uri.parse(WebpageServingRule.urlFor("audio.html"))
                )
                expectThat(tabs.map { it.isSelected }).containsExactly(false, false, true)
            }

            // Open Zero Query and click on the suggested site added by the visit to [firstUrl].
            clickOnUrlBar()
            waitForNode(hasText("Page 1")).performClick()

            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 3)
            activity.webLayerModel.currentBrowser.orderedTabList.value.let { tabs ->
                expectThat(tabs.map { it.url }).containsExactly(
                    Uri.parse(WebpageServingRule.urlFor("")),
                    Uri.parse(WebpageServingRule.urlFor("big_link_element.html")),
                    Uri.parse(WebpageServingRule.urlFor("audio.html"))
                )
                expectThat(tabs.map { it.isSelected }).containsExactly(false, true, false)
            }
        }
    }
}
