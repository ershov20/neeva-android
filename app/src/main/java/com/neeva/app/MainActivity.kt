// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.firstrun.FirstRunActivity
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.firstrun.LoginCallbackIntentParams
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.WelcomeFlowActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * Activity that takes incoming Intents and sends them to the right places.
 * If the user has not yet finished First Run, this will send the user there to make sure that they
 * see any required dialogs.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var applicationCoroutineScope: CoroutineScope
    @Inject lateinit var dispatchers: Dispatchers
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var loginToken: LoginToken
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var popupModel: PopupModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loginHandled = handleLoginIntentIfExists(intent)
        if (!loginHandled) {
            launchNeevaActivity()
        }
    }

    private fun launchNeevaActivity() {
        // TODO(kobec): Remove FirstRunActivity when Premium is ready.
        val activityClass = if (firstRunModel.mustShowFirstRun()) {
            FirstRunActivity::class.java
        } else {
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

    /** Returns false if it did not handle the login intent. FirstRun will not use this codepath. */
    private fun handleLoginIntentIfExists(intent: Intent): Boolean {
        if (intent.dataString == null || Uri.parse(intent.dataString).scheme != "neeva") {
            return false
        }

        val params = LoginCallbackIntentParams.fromLoginCallbackIntent(intent) ?: return false
        if (params.sessionKey == null || params.retryCode != null) {
            popupModel.showSnackbar(getString(params.getErrorResourceId()))
            return false
        }

        val activityClass = getActivityClassToOpen(firstRunModel.getActivityToReturnToAfterLogin())
        val screenToReturnTo = getScreenToOpen(
            activityClass = activityClass,
            screenName = firstRunModel.getScreenToReturnToAfterLogin()
        )

        // If the SharedPreference value is outdated because we renamed a class name or screen name,
        // clear out the outdated values and launch the app normally.
        if (activityClass == null || screenToReturnTo == null) {
            firstRunModel.clearDestinationsToReturnAfterLogin()
            return false
        }

        loginToken.updateCachedCookie(params.sessionKey)
        // Since it is possible that we don't start the NeevaActivity, we need to make sure that the
        // browser cookie jar has the new cached cookie value.
        loginToken.updateBrowserCookieJarWithCachedCookie()
        firstRunModel.queueFetchNeevaInfo()

        val newIntent = Intent(this@MainActivity, activityClass).setAction(Intent.ACTION_MAIN)
        launchActivity(newIntent)
        return true
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

        return destinationValues.find { it.name == screenName }?.name
    }

    private fun launchActivity(intent: Intent) {
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
