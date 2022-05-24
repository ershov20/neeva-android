package com.neeva.app.browsing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.createMainIntent
import com.neeva.app.createNeevaActivityAndroidComposeTestRule
import com.neeva.app.openCardGrid
import com.neeva.app.openLazyTab
import com.neeva.app.waitForActivityStartup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SuppressWarnings("deprecation")
@RunWith(AndroidJUnit4::class)
class IncognitoTabSwitcherTest {
    @get:Rule(order = 0)
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 1)
    val androidComposeRule = createNeevaActivityAndroidComposeTestRule(createMainIntent())

    @Test
    fun incognitoLazyTab_createsNewIncognitoTab() {
        val scenario = androidComposeRule.activityRule.scenario
        val resources = androidComposeRule.activity.resources

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()

        // Create a new tab to nowhere in particular.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.openLazyTab("http://127.0.0.1?regular")

        // Confirm that we see two regular tabs.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(2)

        // Switch to the Incognito screen and confirm that we see an empty incognito grid.
        androidComposeRule.openCardGrid(incognito = true)
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.empty_incognito_tabs_title))
            .assertExists()

        // Open a lazy new tab to nowhere in particular.
        androidComposeRule.openLazyTab("http://127.0.0.1?incognito")

        // Confirm that we have one incognito tab and two regular tabs.
        androidComposeRule.openCardGrid(incognito = true)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(1)
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.onAllNodesWithTag("TabCard").assertCountEquals(2)

        scenario.close()
    }
}
