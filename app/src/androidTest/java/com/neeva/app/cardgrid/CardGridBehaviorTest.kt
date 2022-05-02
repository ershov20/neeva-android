package com.neeva.app.cardgrid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.settings.SettingsToggle
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CardGridBehaviorTest {
    @get:Rule(order = 0)
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 1)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun staysInCardGridWithoutAnyTabs() {
        // Checks for regressions against https://github.com/neevaco/neeva-android/issues/552
        val scenario = androidComposeRule.activityRule.scenario
        val resources = androidComposeRule.activity.resources

        scenario.moveToState(Lifecycle.State.RESUMED)

        androidComposeRule.waitForIdle()

        // Open the tab switcher.  Because a lot of things are changing under the hood and many
        // recompositions are happening, waitForIdle() ends up being flaky.  Instead, wait until we
        // know we've navigated to the correct screen by looking at the AppNavModel.
        androidComposeRule
            .onNodeWithContentDescription(resources.getString(R.string.toolbar_tab_switcher))
            .performClick()
        androidComposeRule.waitForIdle()
        androidComposeRule.waitUntil(TimeUnit.SECONDS.toMillis(5)) {
            val appNavModel = androidComposeRule.activity.appNavModel
            val currentRoute = appNavModel?.currentDestination?.value?.route
            currentRoute == AppNavDestination.CARD_GRID.route
        }

        // Close all the user's tabs.
        scenario.onActivity {
            // Disable asking the user before closing all their tabs.
            it.settingsDataModel
                .getTogglePreferenceSetter(SettingsToggle.REQUIRE_CONFIRMATION_ON_TAB_CLOSE.key)
                .invoke(false)
        }

        androidComposeRule
            .onNodeWithContentDescription(resources.getString(R.string.toolbar_neeva_menu))
            .performClick()

        androidComposeRule
            .onNodeWithContentDescription(
                resources.getString(R.string.close_all_content_description)
            )
            .performClick()

        // Confirm that we're looking at an empty regular TabGrid.
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.empty_regular_tabs_title))
            .assertExists()

        // Open the settings page from the tab switcher.
        androidComposeRule.apply {
            onNodeWithContentDescription(resources.getString(R.string.toolbar_neeva_menu))
                .performClick()

            onNodeWithContentDescription(resources.getString(R.string.settings)).performClick()

            waitForIdle()

            onNodeWithText(resources.getString(R.string.settings)).assertExists()
        }

        scenario.onActivity {
            it.onBackPressed()
        }

        // Confirm that we're still at the TabGrid.
        androidComposeRule
            .onNodeWithText(resources.getString(R.string.empty_regular_tabs_title))
            .assertExists()

        scenario.close()
    }
}
