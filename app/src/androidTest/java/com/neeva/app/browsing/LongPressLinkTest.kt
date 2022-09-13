// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.loadUrlByClickingOnBar
import com.neeva.app.longPressOnBrowserView
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForBrowserState
import com.neeva.app.waitForNodeWithText
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LongPressLinkTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun longPressImageLink() {
        val imageLinkUrl = WebpageServingRule.urlFor("image_link_element.html")
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

            waitForActivityStartup()
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 1)

            // Load the test webpage up in the existing tab.
            loadUrlByClickingOnBar(imageLinkUrl)

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
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 1)

            // Load the test webpage up in the existing tab.
            loadUrlByClickingOnBar(linkUrl)

            // Long press on the context menu and wait for certain controls to show up.
            longPressOnBrowserView()
            waitForNodeWithText("$linkUrl?page_index=2").assertExists()
            onNodeWithTag("MenuHeaderLabel").assertDoesNotExist()
            onNodeWithTag("MenuHeaderImage").assertDoesNotExist()
        }
    }
}
