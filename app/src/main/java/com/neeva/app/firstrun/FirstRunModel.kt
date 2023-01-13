// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.billing.BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN
import com.neeva.app.billing.BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN
import com.neeva.app.billing.SubscriptionManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.singletabbrowser.SingleTabActivity
import com.neeva.app.type.SubscriptionSource
import com.neeva.app.type.SubscriptionType
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.WelcomeFlowActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

fun interface GoogleSignInAccountProvider {
    fun getGoogleSignInAccount(intent: Intent?): Task<GoogleSignInAccount>
}

data class LoginReturnParams(
    val activityToReturnTo: String,
    val screenToReturnTo: String
)

@Singleton
class FirstRunModel internal constructor(
    private val appContext: Context,
    private val authenticatedApolloWrapper: AuthenticatedApolloWrapper,
    private var clientLogger: ClientLogger,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val googleSignInAccountProvider: GoogleSignInAccountProvider,
    private val loginToken: LoginToken,
    private val neevaConstants: NeevaConstants,
    private val neevaUser: NeevaUser,
    private val oktaSignUpHandler: OktaSignUpHandler,
    private val popupModel: PopupModel,
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val settingsDataModel: SettingsDataModel,
    private val subscriptionManager: SubscriptionManager
) {
    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        authenticatedApolloWrapper: AuthenticatedApolloWrapper,
        clientLogger: ClientLogger,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        loginToken: LoginToken,
        neevaConstants: NeevaConstants,
        neevaUser: NeevaUser,
        oktaSignUpHandler: OktaSignUpHandler,
        popupModel: PopupModel,
        settingsDataModel: SettingsDataModel,
        sharedPreferencesModel: SharedPreferencesModel,
        subscriptionManager: SubscriptionManager
    ) : this(
        appContext = appContext,
        authenticatedApolloWrapper = authenticatedApolloWrapper,
        clientLogger = clientLogger,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        googleSignInAccountProvider = GoogleSignIn::getSignedInAccountFromIntent,
        loginToken = loginToken,
        neevaConstants = neevaConstants,
        neevaUser = neevaUser,
        oktaSignUpHandler = oktaSignUpHandler,
        popupModel = popupModel,
        sharedPreferencesModel = sharedPreferencesModel,
        settingsDataModel = settingsDataModel,
        subscriptionManager = subscriptionManager
    )

    internal val mktEmailOptOutState = mutableStateOf(false)
    fun toggleMarketingEmailOptOut() {
        mktEmailOptOutState.value = !mktEmailOptOutState.value
    }

    companion object {
        private const val PREVIEW_MODE_COUNT_THRESHOLD = 10
        private const val SERVER_CLIENT_ID =
            "892902198757-84tm1f14ne0pa6n3dmeehgeo5mk4mhl9.apps.googleusercontent.com"
        private const val AUTH_PATH_GOOGLE_LOGIN = "login-mobile"
        private const val AUTH_PATH_DEFAULT_LOGIN = "login"

        fun setFirstRunDone(sharedPreferencesModel: SharedPreferencesModel) {
            SharedPrefFolder.FirstRun.FirstRunDone.set(sharedPreferencesModel, true)
            SharedPrefFolder.FirstRun.ShouldLogFirstLogin.set(sharedPreferencesModel, true)
        }

        fun mustShowFirstRun(sharedPreferencesModel: SharedPreferencesModel): Boolean {
            return !SharedPrefFolder.FirstRun.FirstRunDone.get(sharedPreferencesModel)
        }

        fun isNeevaLoginUri(uri: Uri, neevaConstants: NeevaConstants): Boolean {
            return when {
                uri.host != neevaConstants.appHost -> false

                uri.pathSegments.size != 1 -> false
                uri.pathSegments.first() == AUTH_PATH_DEFAULT_LOGIN -> true
                uri.pathSegments.first() == AUTH_PATH_GOOGLE_LOGIN -> true

                else -> false
            }
        }

        /**
         * Returns a URI that we can use to sign the user in or sign the user up via the website.
         *
         * Instead of using Custom Tabs or trying to call Google's login API, this function assumes that
         * the user is trying to log in via a tab inside of our own browser and sets the parameters in a
         * way that the backend expects for a web-based login.
         *
         * @param signup Whether or not the user is signing up for a new account.
         * @param provider Service that the user wants to use when creating an account or signing in.
         * @param oktaLoginHint Indicates what email address the user wants to use when using Okta.
         */
        internal fun getWebAuthUri(
            neevaConstants: NeevaConstants,
            signup: Boolean,
            provider: NeevaUser.SSOProvider,
            oktaLoginHint: String,
        ): Uri {
            return Uri
                .parse(neevaConstants.appURL)
                .buildUpon()
                .path(AUTH_PATH_DEFAULT_LOGIN)
                .appendQueryParameter("provider", provider.url)
                .appendQueryParameter("finalPath", provider.finalPath)
                .appendQueryParameter("signup", signup.toString())
                .apply {
                    if (provider == NeevaUser.SSOProvider.OKTA) {
                        appendQueryParameter("loginHint", oktaLoginHint)
                    }
                }
                .build()
        }
    }

    private lateinit var googleSignInClient: GoogleSignInClient

    /**
     * Returns a URI that we can use to log the user in or sign the user up using Custom Tabs.
     *
     * This version should be used with Custom Tabs or with the native Google login APIs to ensure
     * that tokens provided by Google are sent to the backend to finish authentication.  When using
     * this URI, the backend assumes that the client has performed some part of the authentication
     * process before contacting it and redirects the user accordingly.
     */
    internal fun getAndroidCallbackUri(
        signup: Boolean,
        provider: NeevaUser.SSOProvider,
        mktEmailOptOut: Boolean,
        loginHint: String = "",
        identityToken: String = "",
        authorizationCode: String = ""
    ): Uri {
        val path = if (provider == NeevaUser.SSOProvider.GOOGLE && identityToken.isNotEmpty()) {
            // Used to tell the backend that we're using the native Google login library and have
            // already completed part of the login process.
            AUTH_PATH_GOOGLE_LOGIN
        } else {
            // Standard web oauth login.
            AUTH_PATH_DEFAULT_LOGIN
        }

        val builder = Uri.Builder()
            .scheme("https")
            .authority(neevaConstants.appHost)
            .path(path)
            .appendQueryParameter("provider", provider.url)
            .appendQueryParameter("finalPath", provider.finalPath)
            .appendQueryParameter("signup", signup.toString())
            .appendQueryParameter("ignoreCountryCode", "true")
            .appendQueryParameter("mktEmailOptOut", mktEmailOptOut.toString())
            .appendQueryParameter("loginCallbackType", "android")
            .appendQueryParameter("identityToken", identityToken)
            .appendQueryParameter("authorizationCode", authorizationCode)
        return when (provider) {
            NeevaUser.SSOProvider.OKTA ->
                builder
                    .appendQueryParameter("loginHint", loginHint)
                    .build()
            else -> builder.build()
        }
    }

    fun mustShowFirstRun(): Boolean {
        return mustShowFirstRun(sharedPreferencesModel)
    }

    fun setFirstRunDone() {
        setFirstRunDone(sharedPreferencesModel)
    }

    fun setAdBlockOnboardingPreference() {
        SharedPrefFolder.FirstRun.DidShowAdBlockOnboarding.set(
            sharedPreferencesModel,
            false
        )
    }

    fun shouldLogFirstLogin(): Boolean {
        return SharedPrefFolder.FirstRun.ShouldLogFirstLogin.get(sharedPreferencesModel)
    }

    fun setShouldLogFirstLogin(value: Boolean) {
        SharedPrefFolder.FirstRun.ShouldLogFirstLogin.set(sharedPreferencesModel, value)
    }

    fun shouldShowPreviewPromptForSignedOutQuery(): Boolean {
        val hasSignedInBefore =
            SharedPrefFolder.FirstRun.HasSignedInBefore.get(sharedPreferencesModel)

        // Preview mode is only valid when the user has never signed in before.
        if (hasSignedInBefore) return false

        val previewQueries =
            SharedPrefFolder.FirstRun.PreviewQueryCount.get(sharedPreferencesModel) + 1

        SharedPrefFolder.FirstRun.PreviewQueryCount.set(sharedPreferencesModel, previewQueries)

        return previewQueries % PREVIEW_MODE_COUNT_THRESHOLD == 0
    }

    private fun logEvent(interaction: LogConfig.Interaction) {
        clientLogger.logCounter(interaction, null)
    }

    fun getOnCloseOnboarding(showBrowser: () -> Unit): () -> Unit {
        return {
            showBrowser()
            logEvent(LogConfig.Interaction.AUTH_CLOSE)
        }
    }

    fun openSingleTabActivity(context: Context, uri: Uri) {
        val intent = Intent()
            .setClass(context, SingleTabActivity::class.java)
            .setData(uri)
        if (context !is Activity) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Sends the user to a screen that can be used to sign up or log in to Neeva via a particular
     * identify provider.
     */
    fun launchLoginFlow(
        context: Context,
        launchLoginFlowParams: LaunchLoginFlowParams,
        activityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        when (launchLoginFlowParams.provider) {
            NeevaUser.SSOProvider.MICROSOFT -> {
                logEvent(LogConfig.Interaction.AUTH_SIGN_UP_WITH_MICROSOFT)
            }

            NeevaUser.SSOProvider.GOOGLE -> {
                logEvent(LogConfig.Interaction.AUTH_SIGN_UP_WITH_GOOGLE)
            }

            NeevaUser.SSOProvider.OKTA -> {
                // TODO(danalcantara): Not sure why nothing is logged here.
            }

            else -> {
                // Not possible to log in with other cases.
                return
            }
        }

        val provider = launchLoginFlowParams.provider
        val signup = launchLoginFlowParams.signup
        val emailProvided = launchLoginFlowParams.emailProvided
        val mktEmailOptOut = launchLoginFlowParams.mktEmailOptOut

        val useCustomTabs = settingsDataModel.getSettingsToggleValue(
            SettingsToggle.DEBUG_USE_CUSTOM_TABS_FOR_LOGIN
        )
        val isNativeGoogleLoginEnabled = settingsDataModel.getSettingsToggleValue(
            SettingsToggle.DEBUG_ENABLE_NATIVE_GOOGLE_LOGIN
        )

        if (signup &&
            provider == NeevaUser.SSOProvider.OKTA &&
            emailProvided != null &&
            launchLoginFlowParams.passwordProvided != null
        ) {
            coroutineScope.launch(dispatchers.io) {
                oktaSignUpHandler.createOktaAccount(
                    activityContext = context,
                    popupModel = popupModel,
                    emailProvided = emailProvided,
                    passwordProvided = launchLoginFlowParams.passwordProvided
                )
            }
            return
        } else if (provider == NeevaUser.SSOProvider.GOOGLE && isNativeGoogleLoginEnabled) {
            // Falls back to logging in using Custom Tabs.
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(SERVER_CLIENT_ID)
                .requestServerAuthCode(SERVER_CLIENT_ID, true)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
            activityResultLauncher.launch(googleSignInClient.signInIntent)
        } else if (useCustomTabs) {
            openSingleTabActivity(
                context,
                getAndroidCallbackUri(
                    signup = signup,
                    provider = provider,
                    loginHint = emailProvided ?: "",
                    mktEmailOptOut = mktEmailOptOut
                )
            )
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
                .setData(
                    getWebAuthUri(
                        neevaConstants = neevaConstants,
                        signup = signup,
                        provider = provider,
                        oktaLoginHint = emailProvided ?: ""
                    )
                )
                .setClass(context, NeevaActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun setLoginReturnParams(loginReturnParams: LoginReturnParams) {
        SharedPrefFolder.FirstRun
            .ActivityToReturnToAfterLogin.set(
                sharedPreferencesModel,
                loginReturnParams.activityToReturnTo
            )
        SharedPrefFolder.FirstRun
            .ScreenToReturnToAfterLogin.set(
                sharedPreferencesModel,
                loginReturnParams.screenToReturnTo
            )
    }

    /**
     * Sends the user to a screen that can be used to sign up or log in to Neeva via a particular
     * identify provider.
     */
    fun launchLoginFlow(
        loginReturnParams: LoginReturnParams,
        context: Context,
        launchLoginFlowParams: LaunchLoginFlowParams,
        activityResultLauncher: ActivityResultLauncher<Intent>,
        onPremiumAvailable: () -> Unit,
    ) {
        setLoginReturnParams(loginReturnParams)

        val offers = subscriptionManager.productDetailsFlow.value?.subscriptionOfferDetails

        neevaUser.queueOnSignIn(uniqueJobName = "Welcome Flow: onSuccessfulSignIn") {
            coroutineScope.launch(dispatchers.main) {
                val userInfo = neevaUser.userInfoFlow.value

                val subscriptionSource = userInfo?.subscriptionSource
                val hasValidSubscriptionSource =
                    subscriptionSource == SubscriptionSource.GooglePlay ||
                        subscriptionSource == SubscriptionSource.None

                // TODO(kobec): Check if existing purchases == null too!
                if (
                    userInfo != null &&
                    userInfo.subscriptionType == SubscriptionType.Basic &&
                    hasValidSubscriptionSource &&
                    offers != null && offers.isNotEmpty()
                ) {
                    onPremiumAvailable()
                }
            }
        }

        launchLoginFlow(
            context,
            launchLoginFlowParams,
            activityResultLauncher
        )
    }

    fun getActivityToReturnToAfterLogin(): String {
        return SharedPrefFolder.FirstRun.ActivityToReturnToAfterLogin.get(sharedPreferencesModel)
    }

    fun getScreenToReturnToAfterLogin(): String {
        return SharedPrefFolder.FirstRun.ScreenToReturnToAfterLogin.get(sharedPreferencesModel)
    }

    fun clearDestinationsToReturnAfterLogin() {
        SharedPrefFolder.FirstRun.ActivityToReturnToAfterLogin.set(sharedPreferencesModel, "")
        SharedPrefFolder.FirstRun.ScreenToReturnToAfterLogin.set(sharedPreferencesModel, "")
    }

    fun getLoginReturnParameters(
        setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
        selectedSubscriptionPlanTag: String?,
        initialLoginReturnParams: LoginReturnParams?
    ): LoginReturnParams {
        if (!setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
            val isNewUser = mustShowFirstRun()
            val optedForPremium = selectedSubscriptionPlanTag == ANNUAL_PREMIUM_PLAN ||
                selectedSubscriptionPlanTag == MONTHLY_PREMIUM_PLAN

            if (isNewUser || optedForPremium) {
                return LoginReturnParams(
                    activityToReturnTo = WelcomeFlowActivity::class.java.name,
                    screenToReturnTo = WelcomeFlowActivity.Companion.Destinations
                        .SET_DEFAULT_BROWSER.name
                )
            }
        }

        // If an Activity launched an intent to start the WelcomeFlow, it left an explicit Activity
        // name and screen name to return back to once Login has finished.
        initialLoginReturnParams?.let {
            return it
        }

        return LoginReturnParams(
            activityToReturnTo = WelcomeFlowActivity::class.java.name,
            screenToReturnTo = WelcomeFlowActivity.FINISH_WELCOME_FLOW
        )
    }

    fun queueFetchNeevaInfo() {
        coroutineScope.launch(dispatchers.io) {
            loginToken.cachedValueFlow.collect {
                neevaUser.fetch(apolloWrapper = authenticatedApolloWrapper, context = appContext)
            }
        }
    }

    /**
     * Executes [onSuccess] if we can extract a valid auth uri from the [ActivityResult] and
     * falls back to custom tabs if we can not.
     */
    fun handleLoginActivityResult(
        context: Context,
        result: ActivityResult,
        launchLoginFlowParams: LaunchLoginFlowParams,
        onSuccess: (Uri) -> Unit
    ) {
        extractLoginUri(result, launchLoginFlowParams)
            ?.let { onSuccess(it) }
            ?: run {
                openSingleTabActivity(
                    context = context,
                    uri = getAndroidCallbackUri(
                        signup = launchLoginFlowParams.signup,
                        provider = launchLoginFlowParams.provider,
                        mktEmailOptOut = launchLoginFlowParams.mktEmailOptOut,
                        loginHint = launchLoginFlowParams.emailProvided ?: ""
                    )
                )
            }
    }

    private fun extractLoginUri(
        result: ActivityResult,
        launchLoginFlowParams: LaunchLoginFlowParams,
    ): Uri? {
        try {
            val account = googleSignInAccountProvider.getGoogleSignInAccount(result.data)
            val idToken = account.result.idToken ?: return null
            val authCode = account.result.serverAuthCode ?: return null

            return getAndroidCallbackUri(
                signup = launchLoginFlowParams.signup,
                provider = launchLoginFlowParams.provider,
                mktEmailOptOut = launchLoginFlowParams.mktEmailOptOut,
                identityToken = idToken,
                authorizationCode = authCode
            )
        } catch (throwable: ApiException) {
            Timber.e(t = throwable, message = "Failed to extract signed in account from intent")
            return null
        } catch (throwable: RuntimeException) {
            Timber.e(t = throwable, message = "Login API failure")

            return null
        }
    }
}

data class LaunchLoginFlowParams(
    val provider: NeevaUser.SSOProvider,
    val signup: Boolean,
    val mktEmailOptOut: Boolean,
    val emailProvided: String? = null,
    val passwordProvided: String? = null
)
