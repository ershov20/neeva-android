package com.neeva.app.firstrun

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import com.neeva.app.BaseBrowserTest
import com.neeva.app.MainActivity
import com.neeva.app.MultiActivityTestRule
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.createMainIntent
import com.neeva.app.getString
import com.neeva.app.userdata.NeevaUserToken
import com.neeva.app.waitFor
import com.neeva.app.waitForNodeWithText
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
    val multiActivityTestRule = MultiActivityTestRule()

    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var neevaUserToken: NeevaUserToken

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
        expectThat(multiActivityTestRule.activities.any { it.get() is FirstRunActivity }).isFalse()
        expectThat(multiActivityTestRule.activities.any { it.get() is NeevaActivity }).isTrue()
    }

    @Test
    fun skipFirstRunIfUserTokenIsSet() {
        // Set the user token.  First run shouldn't be shown.
        neevaUserToken.setToken("not a real token, but it's set so first run should get skipped")
        activityTestRule.launchActivity(createMainIntent())
        expectThat(multiActivityTestRule.activities.any { it.get() is FirstRunActivity }).isFalse()
        expectThat(multiActivityTestRule.activities.any { it.get() is NeevaActivity }).isTrue()
    }

    @Test
    fun startsFirstRunIfNecessary() {
        activityTestRule.launchActivity(createMainIntent())
        expectThat(multiActivityTestRule.activities.any { it.get() is FirstRunActivity }).isTrue()
        expectThat(multiActivityTestRule.activities.any { it.get() is NeevaActivity }).isFalse()

        androidComposeRule.apply {
            waitForIdle()
            waitForNodeWithText(getString(R.string.get_started)).performClick()
            waitForNodeWithText(getString(R.string.maybe_later)).performClick()

            // Wait for the browser app to start.
            waitFor { it is NeevaActivity }
        }
    }
}
