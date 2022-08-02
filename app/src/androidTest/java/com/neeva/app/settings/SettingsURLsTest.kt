package com.neeva.app.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.TestNeevaConstantsModule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForUrl
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsURLsTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Before
    override fun setUp() {
        super.setUp()
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)
        }
    }

    private fun clickOnSettingsItem(
        @StringRes labelId: Int,
        expectedUrl: String,
        expectedTabCount: Int
    ) {
        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithTag("SettingsPaneItems")
                .performScrollToNode(hasText(getString(labelId)))
            clickOnNodeWithText(getString(labelId))

            // Should send the user to a new tab for viewing the URL.
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForUrl(expectedUrl)
            expectBrowserState(isIncognito = false, regularTabCount = expectedTabCount)
        }
    }

    @Test
    fun clickLinks() {
        androidComposeRule.apply {
            clickOnSettingsItem(
                labelId = R.string.settings_connected_apps,
                expectedUrl = TestNeevaConstantsModule.neevaConstants.appConnectionsURL,
                expectedTabCount = 2
            )
            clickOnSettingsItem(
                labelId = R.string.settings_invite_friends,
                expectedUrl = TestNeevaConstantsModule.neevaConstants.appReferralURL,
                expectedTabCount = 3
            )
            clickOnSettingsItem(
                labelId = R.string.settings_privacy_policy,
                expectedUrl = TestNeevaConstantsModule.neevaConstants.appPrivacyURL,
                expectedTabCount = 4
            )
            clickOnSettingsItem(
                labelId = R.string.settings_terms,
                expectedUrl = TestNeevaConstantsModule.neevaConstants.appTermsURL,
                expectedTabCount = 5
            )
            clickOnSettingsItem(
                labelId = R.string.settings_account_settings,
                expectedUrl = TestNeevaConstantsModule.neevaConstants.appSettingsURL,
                expectedTabCount = 6
            )
            clickOnSettingsItem(
                labelId = R.string.settings_welcome_tours,
                expectedUrl = TestNeevaConstantsModule.neevaConstants.appWelcomeToursURL,
                expectedTabCount = 7
            )
        }
    }
}
