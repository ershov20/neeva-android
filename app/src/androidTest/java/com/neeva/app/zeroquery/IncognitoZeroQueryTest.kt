package com.neeva.app.zeroquery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.clearUrlBar
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectTabListState
import com.neeva.app.getString
import com.neeva.app.navigateViaUrlBar
import com.neeva.app.onBackPressed
import com.neeva.app.openCardGrid
import com.neeva.app.openLazyTab
import com.neeva.app.typeIntoUrlBar
import com.neeva.app.visitMultipleSitesInSameTab
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeWithContentDescription
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForTitle
import com.neeva.app.waitForUrl
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@HiltAndroidTest
class IncognitoZeroQueryTest : BaseBrowserTest() {
    private val testUrl = WebpageServingRule.urlFor("big_link_element.html")

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
            expectTabListState(isIncognito = false, regularTabCount = 1)
        }
    }

    @Test
    fun hasNoSuggestionsFromRegularProfile() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()

            openCardGrid(incognito = true)
            openLazyTab(testUrl)
            waitForTitle("Page 1")

            // Open the URL bar, which should start out empty.
            clickOnUrlBar()
            waitForNodeWithTag("IncognitoZeroQuery").assertIsDisplayed()

            // THe URL bar should start out empty.
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertTextEquals(getString(R.string.url_bar_placeholder))
                .assertIsDisplayed()

            // Type some text in and confirm that none of the the previously visited sites show up
            // as suggestions.
            typeIntoUrlBar("Page")
            onNodeWithTag("SuggestionList").assertDoesNotExist()

            // The URL box should have what we typed in earlier.
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertTextEquals("Page")
                .assertIsDisplayed()
        }
    }

    @Test
    fun canEditUrlAndNavigate() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()

            openCardGrid(incognito = true)
            openLazyTab(testUrl)
            waitForTitle("Page 1")

            // Open the URL bar and type some text in.
            clickOnUrlBar()
            waitForNodeWithTag("IncognitoZeroQuery").assertIsDisplayed()

            // The URL box should be empty.
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertTextEquals(getString(R.string.url_bar_placeholder))
                .assertIsDisplayed()

            // The current URL should show up as an option for editing.  Clicking on it should copy
            // the URL into the box.
            waitForNodeWithText(getString(R.string.edit_current_url))
                .assertIsDisplayed()
                .performClick()
            waitForNodeWithContentDescription(getString(R.string.url_bar_placeholder))
                .assertTextEquals(testUrl)
                .assertIsDisplayed()

            // Clear the URL and type in a different one.
            val newUrl = WebpageServingRule.urlFor("audio.html")
            clearUrlBar()
            navigateViaUrlBar(newUrl)
            waitForTitle("Audio controls test")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
                expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            }
            expectTabListState(isIncognito = true, regularTabCount = 1, incognitoTabCount = 1)

            // After hitting back, you should be on the previous page and be able to hit forward.
            // However, you shouldn't be able to navigate backward anymore because we opened a lazy
            // tab directly to this page.
            onBackPressed()
            waitForUrl(testUrl)
            waitForTitle("Page 1")
            activity.webLayerModel.currentBrowser.activeTabModel.apply {
                expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
                expectThat(navigationInfoFlow.value.canGoForward).isTrue()
            }
            expectTabListState(isIncognito = true, regularTabCount = 1, incognitoTabCount = 1)
        }
    }
}
