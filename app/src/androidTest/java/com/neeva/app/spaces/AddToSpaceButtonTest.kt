package com.neeva.app.spaces

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.WebpageServingRule
import com.neeva.app.expectTabListState
import com.neeva.app.getString
import com.neeva.app.loadUrlInCurrentTab
import com.neeva.app.longPressOnBrowserView
import com.neeva.app.selectItemFromContextMenu
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForTabListState
import com.neeva.app.waitForTitle
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@HiltAndroidTest
class AddToSpaceButtonTest : BaseBrowserTest() {
    private val testUrl = WebpageServingRule.urlFor("big_link_element.html")

    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    override fun setUp() {
        super.setUp()

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectTabListState(isIncognito = false, regularTabCount = 1)

            // Load the test webpage up in the existing tab.
            loadUrlInCurrentTab(testUrl)
            waitForTitle("Page 1")
            expectTabListState(isIncognito = false, regularTabCount = 1)
        }
    }

    @Test
    fun regularProfile_withoutSigningIn_showsPreview() {
        androidComposeRule.apply {
            // Make sure that we're still in regular mode.
            onNodeWithContentDescription(getString(R.string.incognito)).assertDoesNotExist()

            // Clicking on the "Add to Space" button should show the intro bottom sheet.
            onNodeWithContentDescription(getString(R.string.toolbar_save_to_space)).apply {
                expectThat(
                    fetchSemanticsNode().config.getOrNull(SemanticsProperties.Disabled)
                ).isNull()
                performClick()
            }
            waitForNodeWithText(getString(R.string.space_intro_title)).assertIsDisplayed()
            waitForNodeWithText(getString(R.string.space_intro_body)).assertIsDisplayed()
        }
    }

    @Test
    fun incognitoProfile_disablesButton() {
        androidComposeRule.apply {
            // Open the link in a new child tab via the context menu.  The test website is just a
            // link that spans the entire page.
            longPressOnBrowserView()
            selectItemFromContextMenu(R.string.menu_open_in_new_incognito_tab)
            waitForIdle()

            // Wait until the new incognito tab is created.
            waitForTabListState(
                isIncognito = true,
                expectedIncognitoTabCount = 1,
                expectedRegularTabCount = 1
            )
            waitForTitle("Page 2")
            onNodeWithContentDescription(getString(R.string.incognito)).assertExists()

            // Confirm that the add to space button is disabled.  Clicking should do nothing.
            onNodeWithContentDescription(getString(R.string.toolbar_save_to_space)).apply {
                expectThat(
                    fetchSemanticsNode().config.getOrNull(SemanticsProperties.Disabled)
                ).isNotNull()
                performClick()
            }
            waitForIdle()
            onNodeWithText(getString(R.string.space_intro_title)).assertDoesNotExist()
            onNodeWithText(getString(R.string.space_intro_body)).assertDoesNotExist()
        }
    }
}
