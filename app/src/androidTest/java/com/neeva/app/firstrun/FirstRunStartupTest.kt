package com.neeva.app.firstrun

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import com.neeva.app.BaseBrowserTest
import com.neeva.app.LogMutation
import com.neeva.app.MainActivity
import com.neeva.app.MultiActivityTestRule
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.createMainIntent
import com.neeva.app.getString
import com.neeva.app.userdata.NeevaUserToken
import com.neeva.app.waitFor
import com.neeva.app.waitForNodeWithText
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@HiltAndroidTest
class FirstRunStartupTest : BaseBrowserTest() {
    @Suppress("DEPRECATION")
    private val activityTestRule = androidx.test.rule.ActivityTestRule(
        MainActivity::class.java,
        false,
        false
    )

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    @get:Rule
    val multiActivityTestRule = MultiActivityTestRule()

    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var neevaUserToken: NeevaUserToken
    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper

    private lateinit var testAuthenticatedApolloWrapper: TestAuthenticatedApolloWrapper

    override fun setUp() {
        super.setUp()

        testAuthenticatedApolloWrapper =
            authenticatedApolloWrapper as TestAuthenticatedApolloWrapper
    }

    @get:Rule(order = 10000)
    val androidComposeRule = AndroidComposeTestRule(
        activityRule = TestRule { base, _ -> base },
        activityProvider = { multiActivityTestRule.getLastForegroundActivity()!! }
    )

    @Test
    fun skipFirstRunIfSharedPrefIsSet() {
        // Set the shared preference.  First run shouldn't be shown.
        firstRunModel.setFirstRunDone()

        activityTestRule.launchActivity(createMainIntent())

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is NeevaActivity }
            multiActivityTestRule.activities.apply {
                expectThat(any { it.get() is FirstRunActivity }).isFalse()
                expectThat(any { it.get() is NeevaActivity }).isTrue()
            }
        }
    }

    @Test
    fun skipFirstRunIfUserTokenIsSet() {
        // Set the user token.  First run shouldn't be shown.
        neevaUserToken.setToken("not a real token, but it's set so first run should get skipped")

        activityTestRule.launchActivity(createMainIntent())

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is NeevaActivity }
            multiActivityTestRule.activities.apply {
                expectThat(any { it.get() is FirstRunActivity }).isFalse()
                expectThat(any { it.get() is NeevaActivity }).isTrue()
            }
        }
    }

    @Test
    fun startsFirstRunWhenNecessaryAndSendsLogs() {
        activityTestRule.launchActivity(createMainIntent())

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getFirstRunActivity() != null }
            waitFor { activity is FirstRunActivity }
            multiActivityTestRule.activities.apply {
                expectThat(any { it.get() is FirstRunActivity }).isTrue()
                expectThat(any { it.get() is NeevaActivity }).isFalse()
            }

            waitForIdle()

            // Get past the first screen.
            waitForNodeWithText(getString(R.string.get_started)).performClick()

            // Don't set the default browser.
            waitForNodeWithText(getString(R.string.maybe_later)).performClick()

            // Wait for the browser app to start.
            waitFor { it is NeevaActivity }

            // Because we didn't uncheck the checkbox, we should see logging requests fired.
            waitForIdle()
            waitFor {
                testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations.any {
                    it is LogMutation
                }
            }
        }
    }

    @Test
    fun startsFirstRunWhenNecessary_ifLoggingDisallowed_discardsLogs() {
        activityTestRule.launchActivity(createMainIntent())

        androidComposeRule.apply {
            waitFor { multiActivityTestRule.getFirstRunActivity() != null }
            waitFor { activity is FirstRunActivity }
            multiActivityTestRule.activities.apply {
                expectThat(any { it.get() is FirstRunActivity }).isTrue()
                expectThat(any { it.get() is NeevaActivity }).isFalse()
            }

            waitForIdle()

            // Disallow sending logs.
            waitForNodeWithText(getString(R.string.logging_consent)).performClick()

            // Get past the first screen.
            waitForNodeWithText(getString(R.string.get_started)).performClick()

            // Don't set the default browser.
            waitForNodeWithText(getString(R.string.maybe_later)).performClick()

            // Wait for the browser app to start.
            waitFor { multiActivityTestRule.getNeevaActivity() != null }
            waitFor { activity is NeevaActivity }

            // Because we unchecked the checkbox, we should see no logging requests being sent.
            waitForIdle()
            expectThat(
                testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations.any {
                    it is LogMutation
                }
            ).isFalse()
        }
    }
}
