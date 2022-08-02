package com.neeva.app.zeroquery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectBrowserState
import com.neeva.app.navigateViaUrlBar
import com.neeva.app.visitMultipleSitesInSameTab
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeToDisappear
import com.neeva.app.waitForNodeWithContentDescription
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForNodeWithText
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class RegularProfileZeroQueryTest : BaseBrowserTest() {
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

    @Test
    fun suggestedSearchesSectionExpandsAndCollapses() {
        val suggestions = DefaultSuggestions.DEFAULT_SEARCH_SUGGESTIONS
        androidComposeRule.apply {
            clickOnUrlBar()

            waitForNodeWithTag("RegularProfileZeroQuery").apply {
                performScrollToNode(hasText(suggestions[0]))
                performScrollToNode(hasText(suggestions[1]))
                performScrollToNode(hasText(suggestions[2]))
            }

            // Collapse the section.
            waitForNodeWithContentDescription(
                activity.getString(
                    R.string.section_collapse,
                    activity.getString(R.string.searches)
                )
            ).performClick()
            waitForNodeToDisappear(onNodeWithText(suggestions[0]))
            waitForNodeToDisappear(onNodeWithText(suggestions[1]))
            waitForNodeToDisappear(onNodeWithText(suggestions[2]))

            // Confirm that leaving Zero Query and re-opening it will keep the section collapsed.
            navigateViaUrlBar(WebpageServingRule.urlFor("big_link_element.html"))
            clickOnUrlBar()

            // Expand the section.
            waitForNodeWithContentDescription(
                activity.getString(
                    R.string.section_fully_expand,
                    activity.getString(R.string.searches)
                )
            ).performClick()
            waitForNodeWithTag("RegularProfileZeroQuery").apply {
                performScrollToNode(hasText(suggestions[0]))
                performScrollToNode(hasText(suggestions[1]))
                performScrollToNode(hasText(suggestions[2]))
            }
        }
    }

    @Test
    fun suggestedSitesSectionExpandsAndCollapses() {
        val suggestions = DefaultSuggestions.DEFAULT_SITE_SUGGESTIONS
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()

            // We should have 4 sites in history that get suggested to the user, plus four default
            // site suggestions.
            clickOnUrlBar()

            // The user's history plus one suggestion should fit on screen.
            waitForNodeWithText("Page 1").assertIsDisplayed()
            waitForNodeWithText("Page 2").assertIsDisplayed()
            waitForNodeWithText("Page 3").assertIsDisplayed()
            waitForNodeWithText(suggestions[0].title!!).assertIsDisplayed()
            waitForNodeWithText(suggestions[1].title!!).assertIsNotDisplayed()
            waitForNodeWithText(suggestions[2].title!!).assertIsNotDisplayed()
            waitForNodeWithText(suggestions[3].title!!).assertIsNotDisplayed()

            // Fully expand the section.
            waitForNodeWithContentDescription(
                activity.getString(
                    R.string.section_fully_expand,
                    activity.getString(R.string.suggested_sites)
                )
            ).performClick()

            // All the suggestions should be visible, now.
            waitForNodeWithText("Page 1").assertIsDisplayed()
            waitForNodeWithText("Page 2").assertIsDisplayed()
            waitForNodeWithText("Page 3").assertIsDisplayed()
            waitForNodeWithText(suggestions[0].title!!).assertIsDisplayed()
            waitForNodeWithText(suggestions[1].title!!).assertIsDisplayed()
            waitForNodeWithText(suggestions[2].title!!).assertIsDisplayed()
            waitForNodeWithText(suggestions[3].title!!).assertIsDisplayed()

            // Collapse the section.
            waitForNodeWithContentDescription(
                activity.getString(
                    R.string.section_collapse,
                    activity.getString(R.string.suggested_sites)
                )
            ).performClick()

            // None of the sites should be visible.
            waitForNodeToDisappear(onNodeWithText("Page 1"))
            waitForNodeToDisappear(onNodeWithText("Page 2"))
            waitForNodeToDisappear(onNodeWithText("Page 3"))
            waitForNodeToDisappear(onNodeWithText(suggestions[0].title!!))
            waitForNodeToDisappear(onNodeWithText(suggestions[1].title!!))
            waitForNodeToDisappear(onNodeWithText(suggestions[2].title!!))
            waitForNodeToDisappear(onNodeWithText(suggestions[3].title!!))
        }
    }
}
