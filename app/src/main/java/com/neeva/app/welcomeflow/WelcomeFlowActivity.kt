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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
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
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.login.CreateAccountScreen
import com.neeva.app.welcomeflow.login.SignInScreen
import com.neeva.app.welcomeflow.login.WelcomeFlowNavModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
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
        const val SEND_USER_TO_BROWSER_KEY = "SEND_USER_TO_BROWSER"
        const val SELECTED_SUBSCRIPTION_TAG_KEY: String = "SELECTED_SUBSCRIPTION_TAG"
        // endregion

        // region LoginReturnParams Intent keys
        // Provides keys to store strings in an Intent that other activities use to launch this
        // login flow.
        const val ACTIVITY_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY = "ACTIVITY_TO_RETURN_TO"
        const val SCREEN_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY = "SCREEN_TO_RETURN_TO"
        /** Determines if a user wants to use this activity only for signing in. */
        const val SIGN_IN_ONLY = "SIGN_IN_ONLY"
        // endregion

        // Use this as a Screen name parameter to tell this activity to finish after login.
        const val FINISH_WELCOME_FLOW = "FINISH_WELCOME_FLOW"
    }

    @Inject lateinit var activityStarter: ActivityStarter
    @Inject lateinit var billingClientController: BillingClientController
    @Inject lateinit var clientLogger: ClientLogger
    @Inject lateinit var dispatchers: Dispatchers
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var historyDatabase: HistoryDatabase
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var settingsDataModel: SettingsDataModel
    @Inject lateinit var subscriptionManager: SubscriptionManager

    private lateinit var setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager
    private lateinit var navHost: NavHostController
    private lateinit var welcomeFlowNavModel: WelcomeFlowNavModel
    private var startDestination = Destinations.WELCOME.name

    private var sendUserToBrowserOnResume: Boolean = false
    private var selectedSubscriptionPlanTag: String? = null
    private var initialLoginParams = mutableStateOf<LoginReturnParams?>(null)

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDefaultAndroidBrowserManager = SetDefaultAndroidBrowserManager.create(
            activity = this,
            neevaConstants = neevaConstants,
            clientLogger = clientLogger
        )

        savedInstanceState?.let {
            sendUserToBrowserOnResume = it.getBoolean(SEND_USER_TO_BROWSER_KEY)
            selectedSubscriptionPlanTag = it.getString(SELECTED_SUBSCRIPTION_TAG_KEY)
        }

        processIntentParams(intent)

        if (firstRunModel.mustShowFirstRun()) {
            lifecycleScope.launch(dispatchers.io) {
                historyDatabase.hostInfoDao().initializeForFirstRun(neevaConstants)
            }
            clientLogger.logCounter(LogConfig.Interaction.FIRST_RUN_IMPRESSION, null)
            clientLogger.logCounter(LogConfig.Interaction.GET_STARTED_IN_WELCOME, null)
        }

        setupStartScreen()

        // opt new users into strict mode and show ad block onboarding
        // TODO: remove this when we figure out how we persist default values for new users
        settingsDataModel.setContentFilterStrength(
            ContentFilterModel.BlockingStrength.TRACKER_REQUEST
        )
        firstRunModel.setAdBlockOnboardingPreference()

        val onBack = { onBackPressedDispatcher.onBackPressed() }

        setContent {
            NeevaTheme {
                navHost = rememberAnimatedNavController()
                welcomeFlowNavModel = remember(navHost) {
                    WelcomeFlowNavModel(navHost)
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
                        startDestination = startDestination,
                        enterTransition = {
                            Transitions.slideIn(this, AnimatedContentScope.SlideDirection.Start)
                        },
                        exitTransition = {
                            Transitions.slideOut(this, AnimatedContentScope.SlideDirection.Start)
                        },
                        popEnterTransition = {
                            EnterTransition.None
                        },
                        popExitTransition = {
                            ExitTransition.None
                        }
                    ) {
                        composable(Destinations.WELCOME.name) {
                            WelcomeScreen(
                                onContinueInWelcomeScreen = ::onContinueInWelcomeScreen,
                                navigateToSignIn = welcomeFlowNavModel::showSignIn
                            )
                        }

                        composable(Destinations.PLANS.name) {
                            PlansScreen(
                                onContinue = ::onContinueInPlansScreen,
                                navigateToSignIn = welcomeFlowNavModel::showSignIn,
                                saveSubscriptionPlanChoice = { tag ->
                                    selectedSubscriptionPlanTag = tag
                                },
                                onBack = onBack.takeIf { !firstRunModel.mustShowFirstRun() },
                                isSignedOut = neevaUser.isSignedOut(),
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
                                finishWelcomeFlow = ::finishWelcomeFlow
                            )

                            LaunchedEffect(Unit) {
                                clientLogger.logCounter(
                                    path = LogConfig.Interaction
                                        .DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_IMP,
                                    attributes = null
                                )
                            }
                        }

                        composable(
                            route = Destinations.CREATE_ACCOUNT_WITH_GOOGLE.name,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None }
                        ) {
                            CreateAccountScreen(
                                loginReturnParams = getLoginReturnParams(initialLoginParams.value),
                                onPremiumAvailable = {
                                    subscriptionManager.buy(
                                        this@WelcomeFlowActivity,
                                        selectedSubscriptionPlanTag
                                    )
                                },
                                onShowOtherSignUpOptions = {
                                    welcomeFlowNavModel.showCreateAccountWithOther()
                                },
                                navigateToSignIn = welcomeFlowNavModel::showSignIn,
                                onBack = onBack,
                            )
                            LaunchedEffect(true) {
                                clientLogger.logCounter(
                                    LogConfig.Interaction.AUTH_IMPRESSION_LANDING,
                                    null
                                )
                            }
                        }

                        composable(
                            route = Destinations.CREATE_ACCOUNT_WITH_OTHER.name,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None }
                        ) {
                            CreateAccountScreen(
                                loginReturnParams = getLoginReturnParams(initialLoginParams.value),
                                onPremiumAvailable = {
                                    subscriptionManager.buy(
                                        this@WelcomeFlowActivity,
                                        selectedSubscriptionPlanTag
                                    )
                                },
                                navigateToSignIn = welcomeFlowNavModel::showSignIn,
                                onBack = onBack,
                            )

                            LaunchedEffect(true) {
                                clientLogger.logCounter(
                                    LogConfig.Interaction.AUTH_IMPRESSION_OTHER,
                                    null
                                )
                            }
                        }

                        composable(
                            route = Destinations.SIGN_IN.name,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None }
                        ) {
                            SignInScreen(
                                loginReturnParams = getLoginReturnParams(initialLoginParams.value),
                                onPremiumAvailable = {
                                    subscriptionManager.buy(
                                        this@WelcomeFlowActivity,
                                        selectedSubscriptionPlanTag
                                    )
                                },
                                navigateToCreateAccount = {
                                    welcomeFlowNavModel.showCreateAccountWithGoogle()
                                },
                                onBack = onBack,
                            )

                            LaunchedEffect(true) {
                                clientLogger.logCounter(
                                    LogConfig.Interaction.AUTH_IMPRESSION_SIGN_IN,
                                    null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isPremiumPurchaseAvailable(): Boolean {
        val offers = subscriptionManager.productDetailsFlow.value
            ?.subscriptionOfferDetails
        val billingEnabledInSettings = settingsDataModel.getSettingsToggleValue(
            SettingsToggle.DEBUG_ENABLE_BILLING
        )
        return offers != null && offers.isNotEmpty() && billingEnabledInSettings
    }

    private fun processIntentParams(intent: Intent?) {
        val activityToReturnToAfterWelcomeFlow = intent?.extras?.getString(
            ACTIVITY_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY
        )
        val screenToReturnToAfterWelcomeFlow = intent?.extras?.getString(
            SCREEN_TO_RETURN_TO_AFTER_WELCOMEFLOW_KEY
        )
        if (
            activityToReturnToAfterWelcomeFlow != null &&
            screenToReturnToAfterWelcomeFlow != null
        ) {
            initialLoginParams.value = LoginReturnParams(
                activityToReturnTo = activityToReturnToAfterWelcomeFlow,
                screenToReturnTo = screenToReturnToAfterWelcomeFlow
            )
        }

        val usingActivityForSignIn = intent?.extras?.getBoolean(SIGN_IN_ONLY) ?: false
        // SignUp includes showing the user:
        // Plans Screen
        // Create Account Screen
        // Set Default Browser Screen (if it is necessary)
        val usingActivityForSignUp = !usingActivityForSignIn
        val canPurchasePremium = isPremiumPurchaseAvailable()
        startDestination = when {
            firstRunModel.mustShowFirstRun() -> Destinations.WELCOME.name

            usingActivityForSignUp -> {
                // If a user wants to sign up (which is different from sign-in), show Premium plans
                // before sign-up.
                if (canPurchasePremium) {
                    Destinations.PLANS.name
                } else {
                    // If a user cannot purchase Premium, skip to signup screen.
                    Destinations.CREATE_ACCOUNT_WITH_GOOGLE.name
                }
            }

            // If a user strictly wants to only sign (or any other case), let them use this activity
            // for strictly sign-in.
            else -> Destinations.SIGN_IN.name
        }
    }

    private fun getLoginReturnParams(
        initialLoginReturnParams: LoginReturnParams?
    ): LoginReturnParams {
        return firstRunModel.getLoginReturnParameters(
            setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
            selectedSubscriptionPlanTag = selectedSubscriptionPlanTag,
            initialLoginReturnParams = initialLoginReturnParams
        )
    }

    private fun setupStartScreen() {
        val screenToReturnToAfterLogin = firstRunModel.getScreenToReturnToAfterLogin()
        firstRunModel.clearDestinationsToReturnAfterLogin()

        val loginHappened = !neevaUser.isSignedOut()

        when {
            // If there was a login, navigate to the specified screen.
            loginHappened && isValidDestination(screenToReturnToAfterLogin) -> {
                if (this::navHost.isInitialized) {
                    navHost.navigate(screenToReturnToAfterLogin)
                } else {
                    // Since the activity could have died, the navHost might need to be
                    // reinitialized. Make sure it starts on the specified screen.
                    startDestination = screenToReturnToAfterLogin
                }
            }

            loginHappened && screenToReturnToAfterLogin == FINISH_WELCOME_FLOW -> {
                finishWelcomeFlow()
            }

            // If there's no specified login screen to return to:
            screenToReturnToAfterLogin.isEmpty() && isValidDestination(startDestination) -> {
                // Ensure that the start screen is correct. Since this activity can still be alive
                // after being used, it still holds the last destination we were on.
                // It needs to be reset.
                if (
                    this::navHost.isInitialized &&
                    navHost.currentBackStackEntry?.destination?.route != startDestination
                ) {
                    navHost.navigate(startDestination) {
                        launchSingleTop = true

                        popUpTo(startDestination)
                    }
                }
            }

            // There are no valid screens to return to, so no manual setting of which screen to
            // start on is needed.
            else -> { }
        }
    }

    private fun isValidDestination(route: String): Boolean {
        return Destinations.values().any { it.name == route }
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
        setupStartScreen()
        processIntentParams(intent = intent)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean(SEND_USER_TO_BROWSER_KEY, sendUserToBrowserOnResume)
        if (selectedSubscriptionPlanTag != null) {
            outState.putString(SELECTED_SUBSCRIPTION_TAG_KEY, selectedSubscriptionPlanTag)
        }

        initialLoginParams.value?.let {
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

    private fun onContinueInWelcomeScreen() {
        when {
            isPremiumPurchaseAvailable() -> {
                welcomeFlowNavModel.showPlans()
            }
            !setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser() -> {
                welcomeFlowNavModel.showSetDefaultBrowser()
            }
            else -> {
                finishWelcomeFlow()
            }
        }
    }

    private fun onContinueInPlansScreen() {
        when {
            neevaUser.isSignedOut() -> {
                welcomeFlowNavModel.showCreateAccountWithGoogle()
            }
            !setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser() -> {
                welcomeFlowNavModel.showSetDefaultBrowser()
            }
            else -> {
                finishWelcomeFlow()
            }
        }
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
        val returnToActivityAndScreen = initialLoginParams.value != null

        val browserIntent = intent.apply {
            setClass(this@WelcomeFlowActivity, NeevaActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            when {
                returnToActivityAndScreen -> {
                    action = NeevaActivity.ACTION_SHOW_SCREEN
                    initialLoginParams.value?.let {
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
        activityStarter.safeStartActivityForIntent(browserIntent, options = options)
        finishAndRemoveTask()
    }
}
