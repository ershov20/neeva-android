package com.neeva.app.browsing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.getString
import com.neeva.app.openCardGrid
import com.neeva.app.openLazyTab
import com.neeva.app.waitForActivityStartup
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@SuppressWarnings("deprecation")
@HiltAndroidTest
class IncognitoTabSwitcherTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun incognitoLazyTab_createsNewIncognitoTab() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Create a new tab to nowhere in particular.
            openCardGrid(incognito = false)
            openLazyTab(WebpageServingRule.urlFor("?regular"))

            // Confirm that we see two regular tabs.
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(2)

            // Switch to the Incognito screen and confirm that we see an empty incognito grid.
            openCardGrid(incognito = true)
            onNodeWithText(getString(R.string.tab_switcher_no_incognito_tabs)).assertExists()

            // Open a lazy new tab to nowhere in particular.
            openLazyTab(WebpageServingRule.urlFor("?incognito"))

            // Confirm that we have one incognito tab and two regular tabs.
            openCardGrid(incognito = true)
            onAllNodesWithTag("TabCard").assertCountEquals(1)
            openCardGrid(incognito = false)
            onAllNodesWithTag("TabCard").assertCountEquals(2)
        }
    }
}
