package com.neeva.app.spaces

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.neeva.app.AddToSpaceMutation
import com.neeva.app.BaseBrowserTest
import com.neeva.app.DeleteSpaceResultByURLMutation
import com.neeva.app.ListSpacesQuery
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.UserInfoQuery
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.expectTabListState
import com.neeva.app.getString
import com.neeva.app.loadUrlInCurrentTab
import com.neeva.app.longPressOnBrowserView
import com.neeva.app.selectItemFromContextMenu
import com.neeva.app.userdata.NeevaUserToken
import com.neeva.app.waitFor
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForBrowserState
import com.neeva.app.waitForNode
import com.neeva.app.waitForNodeToDisappear
import com.neeva.app.waitForNodeWithContentDescription
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForTitle
import com.neeva.testcommon.WebpageServingRule
import com.neeva.testcommon.apollo.MockListSpacesQueryData
import com.neeva.testcommon.apollo.RESPONSE_USER_INFO_QUERY
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@HiltAndroidTest
class AddToSpaceTest : BaseBrowserTest() {
    private val testUrl = WebpageServingRule.urlFor("big_link_element.html")

    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var neevaUserToken: NeevaUserToken

    private lateinit var testAuthenticatedApolloWrapper: TestAuthenticatedApolloWrapper

    override fun setUp() {
        super.setUp()

        testAuthenticatedApolloWrapper =
            authenticatedApolloWrapper as TestAuthenticatedApolloWrapper
    }

    private fun startAppAndLoadWebPage() {
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
        startAppAndLoadWebPage()

        androidComposeRule.apply {
            // Make sure that we're still in regular mode.
            onNodeWithContentDescription(
                getString(R.string.tracking_protection_incognito_content_description)
            ).assertDoesNotExist()

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
    fun regularProfile_afterSignIn_showsSpaces() {
        neevaUserToken.setToken("Fake user token")

        // Add a fake response that returns two spaces, but only one is editable by the user.
        testAuthenticatedApolloWrapper.registerTestResponse(
            UserInfoQuery(),
            RESPONSE_USER_INFO_QUERY
        )
        testAuthenticatedApolloWrapper.registerTestResponse(
            ListSpacesQuery(),
            MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
        )
        testAuthenticatedApolloWrapper.registerTestResponse(
            MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
            MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE,
        )

        val expectedSpace = MockListSpacesQueryData.SPACE_1.toSpace(
            userId = RESPONSE_USER_INFO_QUERY.user!!.id
        )!!

        startAppAndLoadWebPage()

        androidComposeRule.apply {
            // Make sure that we're still in regular mode.
            onNodeWithContentDescription(
                getString(R.string.tracking_protection_incognito_content_description)
            ).assertDoesNotExist()

            // Confirm that the URL isn't in any of the user's Spaces.
            waitForNodeWithTag("NOT IN SPACE").assertIsDisplayed()
            waitForNodeToDisappear(onNodeWithTag("IS IN SPACE"))

            // Clicking on the "Add to Space" button should show user's spaces.
            waitForNodeWithContentDescription(getString(R.string.toolbar_save_to_space))
                .performClick()

            waitForNodeWithText(expectedSpace.name).assertIsDisplayed()
            onNodeWithText(MockListSpacesQueryData.SPACE_2.space!!.name!!).assertDoesNotExist()

            val addToSpaceMutation = SpaceStore.createAddToSpaceMutation(
                space = expectedSpace,
                url = activity.webLayerModel.currentBrowser.activeTabModel.urlFlow.value,
                title = activity.webLayerModel.currentBrowser.activeTabModel.titleFlow.value
            )
            testAuthenticatedApolloWrapper.registerTestResponse(
                addToSpaceMutation,
                AddToSpaceMutation.Data(entityId = "unused id")
            )

            // Clicking on the available space should fire a request to add it to the user's Space.
            val addDescription =
                activity.getString(R.string.space_add_url_to_space, expectedSpace.name)
            val addMatcher = hasText(expectedSpace.name).and(hasContentDescription(addDescription))
            waitForNode(addMatcher).performClick()
            waitFor {
                testAuthenticatedApolloWrapper.testApolloClientWrapper
                    .performedOperations
                    .contains(addToSpaceMutation)
            }

            // Make sure the button is displayed as filled now.
            waitForNodeWithTag("IS IN SPACE").assertIsDisplayed()
            waitForNodeToDisappear(onNodeWithTag("NOT IN SPACE"))

            // Wait for the snackbar to go away.
            val addedSnackbar =
                activity.getString(R.string.space_added_url_to_space, expectedSpace.name)
            waitForNodeWithText(addedSnackbar).assertIsDisplayed()
            waitForNodeToDisappear(onNodeWithText(addedSnackbar))

            // Clicking on the "Add to Space" button should show user's spaces again.
            val removeDescription =
                activity.getString(R.string.space_remove_url_from_space, expectedSpace.name)
            val removeMatcher = hasText(expectedSpace.name)
                .and(hasContentDescription(removeDescription))
            waitForNodeWithContentDescription(getString(R.string.toolbar_save_to_space))
                .performClick()
            waitForNode(removeMatcher).assertIsDisplayed()

            // Respond to the removal request with a success.
            val removalMutation = SpaceStore.createDeleteSpaceResultByURLMutation(
                space = expectedSpace,
                uri = activity.webLayerModel.currentBrowser.activeTabModel.urlFlow.value
            )
            testAuthenticatedApolloWrapper.registerTestResponse(
                removalMutation,
                DeleteSpaceResultByURLMutation.Data(deleteSpaceResultByURL = true)
            )

            waitForNode(removeMatcher).performClick()
            waitFor {
                testAuthenticatedApolloWrapper.testApolloClientWrapper
                    .performedOperations
                    .contains(removalMutation)
            }

            // Wait for the snackbar to go away.
            val removedSnackbar =
                activity.getString(R.string.space_removed_url_from_space, expectedSpace.name)
            waitForNodeWithText(removedSnackbar).assertIsDisplayed()
            waitForNodeToDisappear(onNodeWithText(removedSnackbar))

            // Confirm that the URL isn't in any of the user's Spaces.
            waitForNodeWithTag("NOT IN SPACE").assertIsDisplayed()
            waitForNodeToDisappear(onNodeWithTag("IS IN SPACE"))

            waitForNodeWithContentDescription(getString(R.string.toolbar_save_to_space))
                .performClick()
            waitForNode(addMatcher).assertIsDisplayed()
            onNode(removeMatcher).assertDoesNotExist()
        }
    }

    @Test
    fun incognitoProfile_disablesButton() {
        startAppAndLoadWebPage()

        androidComposeRule.apply {
            // Open the link in a new child tab via the context menu.  The test website is just a
            // link that spans the entire page.
            longPressOnBrowserView()
            selectItemFromContextMenu(R.string.menu_open_in_new_incognito_tab)
            waitForIdle()

            // Wait until the new incognito tab is created.
            waitForBrowserState(
                isIncognito = true,
                expectedNumIncognitoTabs = 1,
                expectedNumRegularTabs = 1
            )
            waitForTitle("Page 2")

            // Makre sure we're in incognito.
            onNodeWithContentDescription(
                getString(R.string.tracking_protection_incognito_content_description)
            ).assertExists()

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
