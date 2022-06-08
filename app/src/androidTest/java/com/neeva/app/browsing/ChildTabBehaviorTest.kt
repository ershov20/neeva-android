package com.neeva.app.browsing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.WebpageServingRule
import com.neeva.app.loadUrlInCurrentTab
import com.neeva.app.longPressOnBrowserView
import com.neeva.app.onBackPressed
import com.neeva.app.selectItemFromContextMenu
import com.neeva.app.waitFor
import com.neeva.app.waitForActivityStartup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class ChildTabBehaviorTest : BaseBrowserTest() {
    private val testUrl = WebpageServingRule.urlFor("big_link_element.html")

    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun createRegularChildTab() {
        val scenario = androidComposeRule.activityRule.scenario

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()
        androidComposeRule.activity.run {
            expectThat(webLayerModel.currentBrowser.orderedTabList.value).hasSize(1)
        }

        // Load the test webpage up in the existing tab.
        androidComposeRule.loadUrlInCurrentTab(testUrl)
        androidComposeRule.activity.run {
            expectThat(webLayerModel.currentBrowser.orderedTabList.value).hasSize(1)
        }

        // Open the link in a new child tab via the context menu.  The test website is just a link
        // that spans the entire page.
        longPressOnBrowserView()
        selectItemFromContextMenu(R.string.menu_open_in_new_tab)
        androidComposeRule.waitForIdle()

        // Wait until the new tab is created.
        androidComposeRule.waitFor {
            androidComposeRule.activity.webLayerModel.currentBrowser.orderedTabList.value.size == 2
        }
    }

    @Test
    fun createIncognitoChildTab() {
        val scenario = androidComposeRule.activityRule.scenario
        val resources = androidComposeRule.activity.resources

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()
        androidComposeRule.activity.run {
            expectThat(webLayerModel.currentBrowser.orderedTabList.value).hasSize(1)
        }

        // Load the test webpage up in the existing tab.
        androidComposeRule.loadUrlInCurrentTab(testUrl)
        androidComposeRule.activity.run {
            expectThat(webLayerModel.currentBrowser.orderedTabList.value).hasSize(1)
        }

        // Open the link in a new child tab via the context menu.  The test website is just a link
        // that spans the entire page.
        longPressOnBrowserView()
        selectItemFromContextMenu(R.string.menu_open_in_new_incognito_tab)
        androidComposeRule.waitForIdle()

        // Wait until the new incognito tab is created.
        androidComposeRule.waitFor {
            val currentBrowser = androidComposeRule.activity.webLayerModel.currentBrowser
            val browsers = androidComposeRule.activity.webLayerModel.browsersFlow.value
            androidComposeRule.activity.run {
                when {
                    !currentBrowser.isIncognito -> false
                    browsers.incognitoBrowserWrapper?.orderedTabList?.value?.size != 1 -> false
                    currentBrowser.activeTabModel.titleFlow.value != "Page 2" -> false
                    else -> true
                }
            }
        }
        androidComposeRule
            .onNodeWithContentDescription(resources.getString(R.string.incognito))
            .assertExists()

        // Make sure we've still only got one regular profile tab open.
        androidComposeRule.runOnUiThread {
            val regularBrowser =
                androidComposeRule.activity.webLayerModel.browsersFlow.value.regularBrowserWrapper
            expectThat(regularBrowser.orderedTabList.value.size).isEqualTo(1)
        }
    }

    @Test
    fun closingChildTabReturnsToParent() {
        val scenario = androidComposeRule.activityRule.scenario

        // Load the test webpage up in the existing tab.
        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()
        androidComposeRule.loadUrlInCurrentTab(testUrl)

        // Open the link in a new child tab via the context menu.  The test website is just a link
        // that spans the entire page.
        longPressOnBrowserView()
        selectItemFromContextMenu(R.string.menu_open_in_new_tab)
        androidComposeRule.waitForIdle()

        // Wait until the new tab is created.
        androidComposeRule.waitFor {
            val currentBrowser = androidComposeRule.activity.webLayerModel.currentBrowser
            when {
                currentBrowser.orderedTabList.value.size != 2 -> false
                currentBrowser.activeTabModel.titleFlow.value != "Page 2" -> false
                else -> true
            }
        }

        // Hit system back to close the tab.  We should end up back on the parent tab.
        androidComposeRule.onBackPressed()

        // We should be back on the parent tab.
        androidComposeRule.waitFor {
            val currentBrowser = androidComposeRule.activity.webLayerModel.currentBrowser
            when {
                currentBrowser.orderedTabList.value.size != 1 -> false
                currentBrowser.activeTabModel.titleFlow.value != "Page 1" -> false
                else -> true
            }
        }
    }
}
