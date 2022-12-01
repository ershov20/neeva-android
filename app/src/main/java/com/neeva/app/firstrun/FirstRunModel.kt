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
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

fun interface GoogleSignInAccountProvider {
    fun getGoogleSignInAccount(intent: Intent?): Task<GoogleSignInAccount>
}

@Singleton
class FirstRunModel internal constructor(
    private var clientLogger: ClientLogger,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val googleSignInAccountProvider: GoogleSignInAccountProvider,
    private val loginToken: LoginToken,
    private val neevaConstants: NeevaConstants,
    private val oktaSignUpHandler: OktaSignUpHandler,
    private val popupModel: PopupModel,
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val settingsDataModel: SettingsDataModel
) {
    @Inject
    constructor(
        clientLogger: ClientLogger,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        loginToken: LoginToken,
        neevaConstants: NeevaConstants,
        oktaSignUpHandler: OktaSignUpHandler,
        popupModel: PopupModel,
        settingsDataModel: SettingsDataModel,
        sharedPreferencesModel: SharedPreferencesModel
    ) : this(
        clientLogger = clientLogger,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        googleSignInAccountProvider = GoogleSignIn::getSignedInAccountFromIntent,
        loginToken = loginToken,
        neevaConstants = neevaConstants,
        oktaSignUpHandler = oktaSignUpHandler,
        popupModel = popupModel,
        sharedPreferencesModel = sharedPreferencesModel,
        settingsDataModel = settingsDataModel
    )

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

        fun mustShowFirstRun(
            sharedPreferencesModel: SharedPreferencesModel,
            loginToken: LoginToken
        ): Boolean {
            val isFirstRunDone = SharedPrefFolder.FirstRun.FirstRunDone.get(sharedPreferencesModel)

            return when {
                // User has already signed in.
                loginToken.isNotEmpty() -> false

                // SharedPreference has been set, so they must have gone through First Run already.
                isFirstRunDone -> false

                // Show First Run.
                else -> true
            }
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
    private fun getCustomTabsLaunchUri(
        signup: Boolean,
        provider: NeevaUser.SSOProvider,
        loginHint: String = "",
        identityToken: String = "",
        authorizationCode: String = ""
    ): Uri {
        val path = if (provider == NeevaUser.SSOProvider.GOOGLE && identityToken.isNotEmpty()) {
            AUTH_PATH_GOOGLE_LOGIN
        } else {
            AUTH_PATH_DEFAULT_LOGIN
        }

        // TODO remove this temporary hack. There is a leftover callback check that doesn't handle
        // Android in the neeva.com/login path. We should add Android case for that path and delete
        // this.
        val callback = if (provider == NeevaUser.SSOProvider.GOOGLE && identityToken.isNotEmpty()) {
            "android"
        } else {
            "ios"
        }
        val builder = Uri.Builder()
            .scheme("https")
            .authority(neevaConstants.appHost)
            .path(path)
            .appendQueryParameter("provider", provider.url)
            .appendQueryParameter("finalPath", provider.finalPath)
            .appendQueryParameter("signup", signup.toString())
            .appendQueryParameter("ignoreCountryCode", "true")
            .appendQueryParameter("loginCallbackType", callback)
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
        return mustShowFirstRun(sharedPreferencesModel, loginToken)
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

    fun openInCustomTabs(context: Context, uri: Uri) {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .intent
        intent.setPackage(
            CustomTabsClient.getPackageName(context, listOf("com.android.chrome"))
        )
        intent.data = uri
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
            // Fallback to custom tabs for Google sign in if the context is not an Activity
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(SERVER_CLIENT_ID)
                .requestServerAuthCode(SERVER_CLIENT_ID, true)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
            activityResultLauncher.launch(googleSignInClient.signInIntent)
        } else if (useCustomTabs) {
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .intent
                .setPackage(CustomTabsClient.getPackageName(context, listOf("com.android.chrome")))
                .setData(
                    getCustomTabsLaunchUri(
                        signup = signup,
                        provider = provider,
                        loginHint = emailProvided ?: ""
                    )
                )
            context.startActivity(intent)
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
                openInCustomTabs(
                    context = context,
                    uri = getCustomTabsLaunchUri(
                        signup = launchLoginFlowParams.signup,
                        provider = launchLoginFlowParams.provider,
                        loginHint = launchLoginFlowParams.emailProvided ?: ""
                    )
                )
            }
    }

    private fun extractLoginUri(
        result: ActivityResult,
        launchLoginFlowParams: LaunchLoginFlowParams,
    ): Uri? {
        val data = result.takeIf { it.resultCode == Activity.RESULT_OK }?.data ?: run {
            Timber.e("ActivityResult was not successful: ${result.resultCode}")
            return null
        }

        try {
            val account = googleSignInAccountProvider.getGoogleSignInAccount(data)
            val idToken = account.result.idToken ?: return null
            val authCode = account.result.serverAuthCode ?: return null

            return getCustomTabsLaunchUri(
                signup = launchLoginFlowParams.signup,
                provider = launchLoginFlowParams.provider,
                identityToken = idToken,
                authorizationCode = authCode
            )
        } catch (e: ApiException) {
            Timber.e("Failed to extract signed in account from intent", e)
            return null
        }
    }
}

data class LaunchLoginFlowParams(
    val provider: NeevaUser.SSOProvider,
    val signup: Boolean,
    val emailProvided: String? = null,
    val passwordProvided: String? = null
)
