package com.neeva.app.cardgrid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.WAIT_TIMEOUT
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.openCardGrid
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CardGridBehaviorTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun staysInCardGridWithoutAnyTabs() {
        // Checks for regressions against https://github.com/neevaco/neeva-android/issues/552
        val scenario = androidComposeRule.activityRule.scenario
        val resources = androidComposeRule.activity.resources

        scenario.moveToState(Lifecycle.State.RESUMED)
        androidComposeRule.waitForActivityStartup()

        scenario.onActivity {
            // Disable asking the user before closing all their tabs.
            it.settingsDataModel
                .getTogglePreferenceSetter(SettingsToggle.REQUIRE_CONFIRMATION_ON_TAB_CLOSE)
                .invoke(false)
        }

        // Close all the user's tabs.
        androidComposeRule.openCardGrid(incognito = false)
        androidComposeRule.openOverflowMenuAndClickItem(R.string.close_all_content_description)

        // Confirm that we're looking at an empty regular TabGrid.
        androidComposeRule.waitUntil(WAIT_TIMEOUT) {
            androidComposeRule.activity.webLayerModel.currentBrowser.hasNoTabs()
        }
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.empty_regular_tabs_title))
            .assertExists()

        // Open the settings.html page from the tab switcher.
        androidComposeRule.openOverflowMenuAndClickItem(R.string.settings)
        androidComposeRule.waitForNavDestination(AppNavDestination.SETTINGS)

        scenario.onActivity {
            it.onBackPressed()
        }

        // Confirm that we're still at the TabGrid.
        androidComposeRule.waitForNavDestination(AppNavDestination.CARD_GRID)
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.empty_regular_tabs_title))
            .assertExists()

        scenario.close()
    }
}
