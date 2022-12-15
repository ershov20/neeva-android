// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityOptionsCompat
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.neeva.app.Dispatchers
import com.neeva.app.LocalClientLogger
import com.neeva.app.LocalDispatchers
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.LocalNeevaUser
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.LocalSubscriptionManager
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.appnav.Transitions
import com.neeva.app.billing.SubscriptionManager
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserPane
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class WelcomeFlowActivity : AppCompatActivity() {
    companion object {
        internal enum class Destinations {
            WELCOME,
            PLANS,
            SET_DEFAULT_BROWSER
        }
    }

    @Inject lateinit var activityStarter: ActivityStarter
    @Inject lateinit var billingClientController: BillingClientController
    @Inject lateinit var clientLogger: ClientLogger
    @Inject lateinit var dispatchers: Dispatchers
    // TODO(kobec): When we eventually delete FirstRunModel, rename this to WelcomeFlowModel
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var historyDatabase: HistoryDatabase
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var settingsDataModel: SettingsDataModel
    @Inject lateinit var subscriptionManager: SubscriptionManager

    private lateinit var setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager

    private var sendUserToBrowserOnResume: Boolean = false
    private val sendUserToBrowserOnResumeKey = "sendUserToBrowserOnResume"

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            sendUserToBrowserOnResume = it.getBoolean(sendUserToBrowserOnResumeKey)
        }

        clientLogger.logCounter(LogConfig.Interaction.FIRST_RUN_IMPRESSION, null)
        clientLogger.logCounter(LogConfig.Interaction.GET_STARTED_IN_WELCOME, null)

        setDefaultAndroidBrowserManager = SetDefaultAndroidBrowserManager.create(
            activity = this,
            neevaConstants = neevaConstants,
            clientLogger = clientLogger
        )

        // opt new users into strict mode and show ad block onboarding
        // TODO: remove this when we figure out how we persist default values for new users
        settingsDataModel.setContentFilterStrength(
            ContentFilterModel.BlockingStrength.TRACKER_REQUEST
        )
        firstRunModel.setAdBlockOnboardingPreference()

        setContent {
            NeevaTheme {
                val navHost = rememberAnimatedNavController()
                CompositionLocalProvider(
                    LocalClientLogger provides clientLogger,
                    LocalDispatchers provides dispatchers,
                    LocalFirstRunModel provides firstRunModel,
                    LocalNeevaConstants provides neevaConstants,
                    LocalNeevaUser provides neevaUser,
                    LocalSettingsDataModel provides settingsDataModel,
                    LocalSubscriptionManager provides subscriptionManager,
                ) {
                    AnimatedNavHost(
                        navController = navHost,
                        modifier = Modifier.fillMaxSize(),
                        startDestination = Destinations.WELCOME.name,
                        enterTransition = {
                            Transitions.slideIn(this, AnimatedContentScope.SlideDirection.Start)
                        },
                        exitTransition = {
                            Transitions.slideOut(this, AnimatedContentScope.SlideDirection.Start)
                        },
                        popEnterTransition = {
                            Transitions.slideIn(this, AnimatedContentScope.SlideDirection.End)
                        },
                        popExitTransition = {
                            Transitions.slideOut(this, AnimatedContentScope.SlideDirection.End)
                        }
                    ) {
                        composable(Destinations.WELCOME.name) {
                            WelcomeScreen(
                                navigateToPlans = { },
                                navigateToSetDefaultBrowser = {
                                    navHost.navigate(Destinations.SET_DEFAULT_BROWSER.name)
                                }
                            )
                        }

                        composable(Destinations.PLANS.name) {
                            BillingScreen(onDismiss = {})
                        }

                        composable(Destinations.SET_DEFAULT_BROWSER.name) {
                            SetDefaultAndroidBrowserPane(
                                clientLogger = clientLogger,
                                onBackPressed = { navHost.popBackStack() },
                                openAndroidDefaultBrowserSettings = {
                                    sendUserToBrowserOnResume = true
                                    try {
                                        activityStarter.safeStartActivityForIntent(
                                            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                        )
                                    } catch (e: ActivityNotFoundException) {
                                        Timber.e("Could not launch settings", e)
                                        sendUserToBrowser()
                                    }
                                },
                                setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
                                showAsDialog = true,
                                onActivityResultCallback = ::sendUserToBrowser
                            ) {
                                sendUserToBrowser()
                            }

                            LaunchedEffect(Unit) {
                                clientLogger.logCounter(
                                    path = LogConfig.Interaction
                                        .DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_IMP,
                                    attributes = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO(kobec): Add billing client stuff
        if (sendUserToBrowserOnResume) {
            sendUserToBrowser()
        }
        if (settingsDataModel.getSettingsToggleValue(SettingsToggle.DEBUG_ENABLE_BILLING)) {
            billingClientController.onResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClientController.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean(sendUserToBrowserOnResumeKey, sendUserToBrowserOnResume)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    private fun sendUserToBrowser() {
        // Prevent First Run from showing again.
        firstRunModel.setFirstRunDone()

        if (setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
            clientLogger.logCounter(LogConfig.Interaction.SET_DEFAULT_BROWSER, null)
        } else {
            clientLogger.logCounter(LogConfig.Interaction.SKIP_DEFAULT_BROWSER, null)
        }
        clientLogger.sendPendingLogs()

        val newIntent = intent.apply {
            setClass(this@WelcomeFlowActivity, NeevaActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            when (intent.action) {
                null, Intent.ACTION_MAIN -> {
                    // If the user isn't trying to open the app for a specific reason, override the
                    // action so the user sees Zero Query after finishing the First Run flow.
                    action = NeevaActivity.ACTION_ZERO_QUERY
                }
            }
        }

        // Nullify the transition animation to hide the fact that we're switching Activities.
        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0).toBundle()
        activityStarter.safeStartActivityForIntent(newIntent, options)
        finishAndRemoveTask()
    }
}
