package com.neeva.app.cardgrid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.getString
import com.neeva.app.onBackPressed
import com.neeva.app.openCardGrid
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForBrowserState
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

    /** Checks for regressions against https://github.com/neevaco/neeva-android/issues/552 */
    @Test
    fun staysInCardGridWithoutAnyTabs() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Close all the user's tabs.
            openCardGrid(incognito = false)
            openOverflowMenuAndClickItem(R.string.close_all_content_description)

            // Confirm that we're looking at an empty regular TabGrid.
            waitForBrowserState(
                isIncognito = false,
                expectedNumRegularTabs = 0,
                expectedNumIncognitoTabs = null
            )
            onNodeWithText(getString(R.string.empty_regular_tabs_title)).assertExists()

            // Open the settings page from the tab switcher.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            // Leave Settings.
            onBackPressed()

            // Confirm that we're still at the TabGrid.
            waitForNavDestination(AppNavDestination.CARD_GRID)
            onNodeWithText(getString(R.string.empty_regular_tabs_title)).assertExists()
        }
    }
}
