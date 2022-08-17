package com.neeva.app.feedback

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import com.apollographql.apollo3.api.Operation
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.SendFeedbackMutation
import com.neeva.app.TestNeevaConstantsModule
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.onBackPressed
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.waitFor
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForUrl
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse

@HiltAndroidTest
class FeedbackViewTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper

    private lateinit var testApolloWrapper: TestAuthenticatedApolloWrapper

    override fun setUp() {
        super.setUp()
        testApolloWrapper = authenticatedApolloWrapper as TestAuthenticatedApolloWrapper
    }

    @Test
    fun visitHelpCenter() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Navigate to Support.
            openOverflowMenuAndClickItem(R.string.feedback)
            waitForNavDestination(AppNavDestination.FEEDBACK)

            // Click on "Visit our help center".  It should open a new tab to load the Neeva URL.
            // Not super happy with this test because is actively loads the real Neeva website.
            waitForNodeWithText(getString(R.string.submit_feedback_help_center_link)).performClick()
            waitForNavDestination(AppNavDestination.BROWSER)
            waitForUrl(TestNeevaConstantsModule.neevaConstants.appHelpCenterURL)
            expectBrowserState(isIncognito = false, regularTabCount = 2)
        }
    }

    @Test
    fun cancelFeedback_showsNoSnackbar() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Navigate to Support.
            openOverflowMenuAndClickItem(R.string.feedback)
            waitForNavDestination(AppNavDestination.FEEDBACK)

            // Send just the user's message.
            waitForNodeWithText(getString(R.string.submit_feedback_view_share_url)).performClick()
            waitForNodeWithText(getString(R.string.submit_feedback_share_screenshot)).performClick()
            waitForNodeWithText(getString(R.string.submit_feedback_textfield_placeholder))
                .performTextInput("Test message")

            // Mock out the response.
            val sendFeedbackMutation =
                FeedbackViewModel.createMutation(
                    user = neevaUser,
                    userFeedback = "Test message",
                    url = null,
                    screenshot = null
                )
            testApolloWrapper.registerTestResponse(
                sendFeedbackMutation,
                SendFeedbackMutation.Data(sendFeedbackV2 = true)
            )

            // Cancel sending the feedback.
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Confirm that no snackbar was displayed
            onNodeWithText(getString(R.string.submit_feedback_acknowledgement))
                .assertDoesNotExist()

            // Confirm that no mutation was fired.
            expectThat(
                testApolloWrapper
                    .testApolloClientWrapper
                    .performedOperations
                    .contains(sendFeedbackMutation)
            ).isFalse()
        }
    }

    @Test
    fun submitTextOnlyFeedback() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Navigate to Support.
            openOverflowMenuAndClickItem(R.string.feedback)
            waitForNavDestination(AppNavDestination.FEEDBACK)

            // Send just the user's message.
            waitForNodeWithText(getString(R.string.submit_feedback_view_share_url)).performClick()
            waitForNodeWithText(getString(R.string.submit_feedback_share_screenshot)).performClick()
            waitForNodeWithText(getString(R.string.submit_feedback_textfield_placeholder))
                .performTextInput("Test message")

            // Mock out the response.
            val sendFeedbackMutation =
                FeedbackViewModel.createMutation(
                    user = neevaUser,
                    userFeedback = "Test message",
                    url = null,
                    screenshot = null
                )
            testApolloWrapper.registerTestResponse(
                sendFeedbackMutation,
                SendFeedbackMutation.Data(sendFeedbackV2 = true)
            )

            // Click the button to submit feedback.
            waitForNodeWithText(getString(R.string.send)).performClick()

            // Confirm that the acknowledgement snackbar appeared.
            waitForNodeWithText(getString(R.string.submit_feedback_acknowledgement))
                .assertIsDisplayed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Confirm that the mutation was fired.
            waitForOperation(sendFeedbackMutation)
        }
    }

    @Test
    fun submitFeedback_withTextAndOriginalUrl() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Navigate to Support.
            openOverflowMenuAndClickItem(R.string.feedback)
            waitForNavDestination(AppNavDestination.FEEDBACK)

            // Send the user's message and the URL, but remove the screenshot.
            waitForNodeWithText(getString(R.string.submit_feedback_share_screenshot)).performClick()
            waitForNodeWithText(getString(R.string.submit_feedback_textfield_placeholder))
                .performTextInput("Test message")

            // Mock out the response.
            val sendFeedbackMutation =
                FeedbackViewModel.createMutation(
                    user = neevaUser,
                    userFeedback = "Test message",
                    url = neevaConstants.appURL,
                    screenshot = null
                )
            testApolloWrapper.registerTestResponse(
                sendFeedbackMutation,
                SendFeedbackMutation.Data(sendFeedbackV2 = true)
            )

            // Click the button to submit feedback.
            waitForNodeWithText(getString(R.string.send)).performClick()

            // Confirm that the acknowledgement snackbar appeared.
            waitForNodeWithText(getString(R.string.submit_feedback_acknowledgement))
                .assertIsDisplayed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Confirm that the mutation was fired.
            waitForOperation(sendFeedbackMutation)
        }
    }

    @Test
    fun submitFeedback_withTextAndEditedUrl() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Navigate to Support.
            openOverflowMenuAndClickItem(R.string.feedback)
            waitForNavDestination(AppNavDestination.FEEDBACK)

            // Send the user's message and an edited URL, but remove the screenshot.
            waitForNodeWithText(getString(R.string.submit_feedback_share_screenshot)).performClick()
            waitForNodeWithTag("Feedback URL").apply {
                performTextClearance()
                performTextInput("Replaced URL")
            }
            waitForNodeWithText(getString(R.string.submit_feedback_textfield_placeholder))
                .performTextInput("Test message")

            // Mock out the response.
            val sendFeedbackMutation =
                FeedbackViewModel.createMutation(
                    user = neevaUser,
                    userFeedback = "Test message",
                    url = "Replaced URL",
                    screenshot = null
                )
            testApolloWrapper.registerTestResponse(
                sendFeedbackMutation,
                SendFeedbackMutation.Data(sendFeedbackV2 = true)
            )

            // Click the button to submit feedback.
            waitForNodeWithText(getString(R.string.send)).performClick()

            // Confirm that the acknowledgement snackbar appeared.
            waitForNodeWithText(getString(R.string.submit_feedback_acknowledgement))
                .assertIsDisplayed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Confirm that the mutation was fired.
            waitForOperation(sendFeedbackMutation)
        }
    }

    @Test
    fun submitFeedback_withEverything() {
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Navigate to Support.
            openOverflowMenuAndClickItem(R.string.feedback)
            waitForNavDestination(AppNavDestination.FEEDBACK)

            // Edit the feedback message.
            waitForNodeWithText(getString(R.string.submit_feedback_textfield_placeholder))
                .performTextInput("Test message")

            // Yank the screenshot out of the FeedbackViewModel directly to construct the mutation,
            // then mock out the response.
            val sendFeedbackMutation =
                FeedbackViewModel.createMutation(
                    user = neevaUser,
                    userFeedback = "Test message",
                    url = neevaConstants.appURL,
                    screenshot = activity.feedbackViewModel.screenshot
                )

            testApolloWrapper.registerTestResponse(
                sendFeedbackMutation,
                SendFeedbackMutation.Data(sendFeedbackV2 = true)
            )

            // Click the button to submit feedback.
            waitForNodeWithText(getString(R.string.send)).performClick()

            // Confirm that the acknowledgement snackbar appeared.
            waitForNodeWithText(getString(R.string.submit_feedback_acknowledgement))
                .assertIsDisplayed()
            waitForNavDestination(AppNavDestination.BROWSER)

            // Confirm that the mutation was fired.
            waitForOperation(sendFeedbackMutation)
        }
    }

    /** Wait for an Apollo operation to be fired. */
    private fun <D : Operation.Data> waitForOperation(operation: Operation<D>) {
        androidComposeRule.apply {
            waitFor {
                testApolloWrapper
                    .testApolloClientWrapper
                    .performedOperations
                    .contains(operation)
            }
        }
    }
}
