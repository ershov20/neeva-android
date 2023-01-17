// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.app.ActivityManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.neeva.app.NeevaActivity.Companion.ACTION_SHOW_SCREEN_AFTER_LOGIN
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.firstrun.LoginCallbackIntentParams
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.singletabbrowser.SingleTabActivity
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.WelcomeFlowActivity
import com.neeva.app.welcomeflow.WelcomeFlowActivity.Companion.FINISH_WELCOME_FLOW
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * Activity that takes incoming Intents and sends them to the right places.
 * If the user has not yet finished First Run, this will send the user there to make sure that they
 * see any required dialogs.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var clientLogger: ClientLogger
    @Inject lateinit var dispatchers: Dispatchers
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var loginToken: LoginToken
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var popupModel: PopupModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loginHandled = handleLoginIntentIfExists(intent)
        if (!loginHandled) {
            // If the SharedPreference value of which Destination to return to after a login is
            // outdated because the login flow never got handled properly, clear out any potential
            // outdated shared preference values and launch the app normally.
            firstRunModel.clearDestinationsToReturnAfterLogin()
            startUpWithoutLoginIntent()
        }
    }

    private fun startUpWithoutLoginIntent() {
        val activityClass = if (loginToken.isEmpty() && firstRunModel.mustShowFirstRun()) {
            WelcomeFlowActivity::class.java
        } else {
            // In the past, for users who downloaded the app before the FirstRunDone
            // SharedPreference existed, we decided to skip FirstRun if the user was logged in.
            // This means that those users have never had the FirstRunDone SharedPreference set to
            // true.
            // To ensure that the FirstRunDone SharedPreference stays up to date with who exactly
            // we want to show FirstRun to, set it to done here.
            firstRunModel.setFirstRunDone()
            NeevaActivity::class.java
        }

        // Sanitize any Intents we receive from external sources before it gets passed along to
        // the rest of our app.
        val newIntent: Intent = intent.sanitized()
            ?: Intent(this@MainActivity, activityClass).setAction(Intent.ACTION_MAIN)
        newIntent.setClass(this@MainActivity, activityClass)

        // Replace the flags because they apply to MainActivity and not child Activities, (e.g.)
        // we don't want the "exclude from recents" flag being applied to children.
        newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        launchActivity(newIntent)
    }

    /** Returns false if it did not handle the login intent. */
    private fun handleLoginIntentIfExists(intent: Intent): Boolean {
        if (intent.dataString == null || Uri.parse(intent.dataString).scheme != "neeva") {
            return false
        }

        val params = LoginCallbackIntentParams.fromLoginCallbackIntent(intent) ?: return false
        if (params.sessionKey == null || params.retryCode != null) {
            popupModel.showSnackbar(getString(params.getErrorResourceId()))
            return false
        }

        // FirstRun will still use this codepath when logging in, but will return false below
        // and execute its own processing of the login cookie in its own FirstRunActivity.
        // This log is left here to make sure that we tracked the first login anyways.
        if (firstRunModel.shouldLogFirstLogin()) {
            clientLogger.logCounter(LogConfig.Interaction.LOGIN_AFTER_FIRST_RUN, null)
            firstRunModel.setShouldLogFirstLogin(false)
        }

        val activityClass = getActivityClassToOpen(firstRunModel.getActivityToReturnToAfterLogin())
        val screenToReturnTo = getScreenToOpen(
            activityClass = activityClass,
            screenName = firstRunModel.getScreenToReturnToAfterLogin()
        )

        if (activityClass == null || screenToReturnTo == null) {
            return false
        }

        loginToken.updateCachedCookie(params.sessionKey)
        // Since it is possible that we don't start the NeevaActivity, we need to make sure that the
        // browser cookie jar has the new cached cookie value.
        loginToken.updateBrowserCookieJarWithCachedCookie(
            onFailure = {
                // Tell the user something went wrong and that they couldn't be signed in.
                popupModel.showSnackbar(getString(R.string.error_generic))
            }
        )
        firstRunModel.queueFetchNeevaInfo()

        // At this point, the SingleTabActivity has no further use to the logged in user.
        // Remove it from Android Recents so the user can't navigate back to it.
        removeSingleTabActivityFromRecents()

        val newIntent = Intent(this@MainActivity, activityClass)
            .setAction(ACTION_SHOW_SCREEN_AFTER_LOGIN)
        // Send the user to a URL created by appending the [finalPath] to
        // the base Neeva URL.
        newIntent.data = Uri.parse(neevaConstants.appURL).buildUpon()
            .apply { params.finalPath?.let { path(it) } }
            .build()

        launchActivity(newIntent)
        return true
    }

    private fun removeSingleTabActivityFromRecents() {
        try {
            val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            val appTasks = am?.appTasks
            appTasks
                ?.filter {
                    it.taskInfo.baseActivity?.className == SingleTabActivity::class.java.name
                }
                ?.forEach { it.finishAndRemoveTask() }
        } catch (e: Exception) {
            Timber.e(
                e,
                "Tried to remove SingleTabActivity from Recents but ran into a problem: "
            )
        }
    }

    private fun getActivityClassToOpen(activityName: String): Class<out AppCompatActivity>? {
        return when (activityName) {
            WelcomeFlowActivity::class.java.name -> WelcomeFlowActivity::class.java
            NeevaActivity::class.java.name -> NeevaActivity::class.java
            else -> null
        }
    }

    private fun getScreenToOpen(
        activityClass: Class<out AppCompatActivity>?,
        screenName: String
    ): String? {
        val destinationValues = when (activityClass) {
            WelcomeFlowActivity::class.java -> WelcomeFlowActivity.Companion.Destinations.values()
            NeevaActivity::class.java -> AppNavDestination.values()
            else -> null
        }

        if (destinationValues == null) {
            return null
        }

        if (activityClass == WelcomeFlowActivity::class.java && screenName == FINISH_WELCOME_FLOW) {
            return FINISH_WELCOME_FLOW
        }

        return destinationValues.find { it.name == screenName }?.name
    }

    private fun launchActivity(intent: Intent) {
        // Nullify the transition animation to hide the fact that we're switching Activities.
        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0).toBundle()
        ContextCompat.startActivity(this, intent, options)
        finishAndRemoveTask()
    }

    /**
     * Checks to see if trying to read the [Intent]'s extras will cause a crash.
     *
     * Returns the original [Intent] if it is safe to use and null if it isn't.
     */
    private fun Intent?.sanitized(): Intent? {
        return try {
            // Check for parceling/unmarshalling errors, which can happen if an external app sends
            // an Intent that contains a Parcelable we can't handle.  The Bundle will get
            // unmarshalled whenever we try to check for any extras from the Bundle, so it doesn't
            // really matter which we pick.
            also { this?.hasExtra(SearchManager.QUERY) }
        } catch (throwable: Exception) {
            Timber.w(t = throwable, message = "Failed to parse Intent; discarding")
            null
        }
    }
}
