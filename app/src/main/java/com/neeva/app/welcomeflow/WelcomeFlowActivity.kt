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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityOptionsCompat
import androidx.navigation.NavHostController
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
import com.neeva.app.billing.BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN
import com.neeva.app.billing.BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN
import com.neeva.app.billing.SubscriptionManager
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.firstrun.LoginReturnParams
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.login.CreateAccountScreen
import com.neeva.app.welcomeflow.login.LoginFlowNavModel
import com.neeva.app.welcomeflow.login.SignInScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class WelcomeFlowActivity : AppCompatActivity() {
    companion object {
        internal enum class Destinations {
            WELCOME,
            PLANS,
            SET_DEFAULT_BROWSER,
            CREATE_ACCOUNT_WITH_GOOGLE,
            CREATE_ACCOUNT_WITH_OTHER,
            SIGN_IN,
        }

        // region SavedInstanceState keys
        // Intended to save state when navigating outside of the Neeva App.
        const val SEND_USER_TO_BROWSER_KEY = "sendUserToBrowserOnResume"
        const val SELECTED_SUBSCRIPTION_TAG_KEY: String = "selectedSubscriptionPlanTag"
        // endregion

        // region LoginReturnParams Intent keys
        // Provides keys to store strings in an Intent that other activities use to launch this
        // login flow.
        const val ACTIVITY_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY = "activityToReturnToAfterWelcomeFlow"
        const val SCREEN_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY = "screenToReturnToAfterWelcomeFlow"
        // endregion

        // Use this as a Screen name parameter to tell this activity to finish after login.
        const val FINISH_WELCOME_FLOW = "FINISH_WELCOME_FLOW"
    }

    @Inject lateinit var activityStarter: ActivityStarter
    @Inject lateinit var billingClientController: BillingClientController
    @Inject lateinit var clientLogger: ClientLogger
    @Inject lateinit var dispatchers: Dispatchers
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var settingsDataModel: SettingsDataModel
    @Inject lateinit var subscriptionManager: SubscriptionManager

    private lateinit var setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager
    private lateinit var navHost: NavHostController

    private var sendUserToBrowserOnResume: Boolean = false
    private var selectedSubscriptionPlanTag: String? = null
    private var loginReturnParams: LoginReturnParams? = null

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            sendUserToBrowserOnResume = it.getBoolean(SEND_USER_TO_BROWSER_KEY)
            selectedSubscriptionPlanTag = it.getString(SELECTED_SUBSCRIPTION_TAG_KEY)
        }

        collectLoginReturnParams()

        // TODO(kobec): Check if the IF statement is correct
        if (firstRunModel.mustShowFirstRun()) {
            clientLogger.logCounter(LogConfig.Interaction.FIRST_RUN_IMPRESSION, null)
            clientLogger.logCounter(LogConfig.Interaction.GET_STARTED_IN_WELCOME, null)
        }

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
                navHost = rememberAnimatedNavController()
                val loginFlowNavModel = remember(navHost) {
                    LoginFlowNavModel(navHost)
                }
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
                                navigateToPlans = {
                                    navHost.navigate(Destinations.PLANS.name)
                                },
                                navigateToSignIn = {
                                    navHost.navigate(Destinations.SIGN_IN.name)
                                },
                                navigateToSetDefaultBrowser = {
                                    navHost.navigate(Destinations.SET_DEFAULT_BROWSER.name)
                                }
                            )
                        }

                        composable(Destinations.PLANS.name) {
                            PlansScreen(
                                navigateToCreateAccount =
                                loginFlowNavModel::navigateToCreateAccountWithGoogle,
                                navigateToSignIn =
                                loginFlowNavModel::navigateToSignIn,
                                saveSubscriptionPlanChoice = { tag ->
                                    selectedSubscriptionPlanTag = tag
                                }
                            )
                        }

                        composable(Destinations.SET_DEFAULT_BROWSER.name) {
                            SetDefaultBrowserScreen(
                                openAndroidDefaultBrowserSettings = {
                                    sendUserToBrowserOnResume = true
                                    try {
                                        activityStarter.safeStartActivityForIntent(
                                            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                        )
                                    } catch (e: ActivityNotFoundException) {
                                        Timber.e("Could not launch settings", e)
                                        finishWelcomeFlow()
                                    }
                                },
                                setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
                                sendUserToBrowser = ::finishWelcomeFlow
                            )

                            LaunchedEffect(Unit) {
                                clientLogger.logCounter(
                                    path = LogConfig.Interaction
                                        .DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_IMP,
                                    attributes = null
                                )
                            }
                        }

                        composable(Destinations.CREATE_ACCOUNT_WITH_GOOGLE.name) {
                            CreateAccountScreen(
                                loginReturnParams = getLoginReturnParameters(
                                    setDefaultAndroidBrowserManager
                                ),
                                onPremiumAvailable = {
                                    subscriptionManager.buy(
                                        this@WelcomeFlowActivity,
                                        selectedSubscriptionPlanTag
                                    )
                                },
                                onShowOtherSignUpOptions =
                                loginFlowNavModel::navigateToCreateAccountWithOther,
                                navigateToSignIn = { navHost.navigate(Destinations.SIGN_IN.name) },
                                onBack = { navHost.popBackStack() },
                            )
                        }

                        composable(Destinations.CREATE_ACCOUNT_WITH_OTHER.name) {
                            CreateAccountScreen(
                                loginReturnParams = getLoginReturnParameters(
                                    setDefaultAndroidBrowserManager
                                ),
                                onPremiumAvailable = {
                                    subscriptionManager.buy(
                                        this@WelcomeFlowActivity,
                                        selectedSubscriptionPlanTag
                                    )
                                },
                                navigateToSignIn = loginFlowNavModel::navigateToSignIn,
                                onBack = { navHost.popBackStack() },
                            )
                        }

                        composable(Destinations.SIGN_IN.name) {
                            SignInScreen(
                                loginReturnParams = getLoginReturnParameters(
                                    setDefaultAndroidBrowserManager
                                ),
                                onPremiumAvailable = {
                                    subscriptionManager.buy(
                                        this@WelcomeFlowActivity,
                                        selectedSubscriptionPlanTag
                                    )
                                },
                                navigateToCreateAccount =
                                loginFlowNavModel::navigateToCreateAccountWithGoogle,
                                onBack = { navHost.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun collectLoginReturnParams() {
        val activityToReturnToAfterWelcomeFlow = intent.extras?.getString(
            ACTIVITY_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY
        )
        val screenToReturnToAfterWelcomeFlow = intent.extras?.getString(
            SCREEN_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY
        )
        if (
            activityToReturnToAfterWelcomeFlow != null &&
            screenToReturnToAfterWelcomeFlow != null
        ) {
            loginReturnParams = LoginReturnParams(
                activityToReturnTo = activityToReturnToAfterWelcomeFlow,
                screenToReturnTo = screenToReturnToAfterWelcomeFlow
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (sendUserToBrowserOnResume) {
            finishWelcomeFlow()
        }
        // TODO(kobec): Remove when welcome flow is ready.
        if (settingsDataModel.getSettingsToggleValue(SettingsToggle.DEBUG_ENABLE_BILLING)) {
            billingClientController.onResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClientController.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val screenToReturnToAfterLogin = firstRunModel.getScreenToReturnToAfterLogin()
        firstRunModel.clearDestinationsToReturnAfterLogin()

        when {
            Destinations.values().any { it.name == screenToReturnToAfterLogin } -> {
                navHost.navigate(screenToReturnToAfterLogin)
            }

            screenToReturnToAfterLogin == FINISH_WELCOME_FLOW -> { finishWelcomeFlow() }

            else -> { }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean(SEND_USER_TO_BROWSER_KEY, sendUserToBrowserOnResume)
        if (selectedSubscriptionPlanTag != null) {
            outState.putString(SELECTED_SUBSCRIPTION_TAG_KEY, selectedSubscriptionPlanTag)
        }

        loginReturnParams?.let {
            outState.putString(
                ACTIVITY_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY,
                it.activityToReturnTo
            )
            outState.putString(
                SCREEN_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY,
                it.screenToReturnTo
            )
        }
        super.onSaveInstanceState(outState, outPersistentState)
    }

    private fun getLoginReturnParameters(
        setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
    ): LoginReturnParams {
        if (!setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
            val isNewUser = firstRunModel.mustShowFirstRun()
            val optedForPremium = selectedSubscriptionPlanTag == ANNUAL_PREMIUM_PLAN ||
                selectedSubscriptionPlanTag == MONTHLY_PREMIUM_PLAN

            if (isNewUser || optedForPremium) {
                return LoginReturnParams(
                    activityToReturnTo = WelcomeFlowActivity::class.java.name,
                    screenToReturnTo = Destinations.SET_DEFAULT_BROWSER.name
                )
            }
        }

        // If an Activity launched an intent to start the WelcomeFlow, it left an explicit Activity
        // name and screen name to return back to once Login has finished.
        loginReturnParams?.let {
            return it
        }

        return LoginReturnParams(
            activityToReturnTo = WelcomeFlowActivity::class.java.name,
            screenToReturnTo = FINISH_WELCOME_FLOW
        )
    }

    private fun finishWelcomeFlow() {
        // Prevent First Run from showing again.
        firstRunModel.setFirstRunDone()

        if (setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
            clientLogger.logCounter(LogConfig.Interaction.SET_DEFAULT_BROWSER, null)
        } else {
            clientLogger.logCounter(LogConfig.Interaction.SKIP_DEFAULT_BROWSER, null)
        }
        clientLogger.sendPendingLogs()

        // An activity outside of WelcomeFlowActivity has triggered the WelcomeFlow via intent:
        val returnToActivityAndScreen = loginReturnParams != null

        val newIntent = intent.apply {
            setClass(this@WelcomeFlowActivity, NeevaActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            when {
                returnToActivityAndScreen -> {
                    action = NeevaActivity.ACTION_SHOW_SCREEN
                    loginReturnParams?.let {
                        firstRunModel.setLoginReturnParams(it)
                    }
                }

                // If the user isn't trying to open the app for a specific reason, override the
                // action so the user sees Zero Query after finishing the First Run flow.
                intent.action == null || intent.action == Intent.ACTION_MAIN -> {
                    action = NeevaActivity.ACTION_ZERO_QUERY
                }
            }
        }

        // Nullify the transition animation to hide the fact that we're switching Activities.
        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0).toBundle()
        activityStarter.safeStartActivityForIntent(newIntent, options = options)
        finishAndRemoveTask()
    }
}
