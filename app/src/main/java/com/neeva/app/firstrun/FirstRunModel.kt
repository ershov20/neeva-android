// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun interface GoogleSignInAccountProvider {
    fun getGoogleSignInAccount(intent: Intent?): Task<GoogleSignInAccount>
}

@Singleton
class FirstRunModel internal constructor(
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val loginToken: LoginToken,
    private val neevaConstants: NeevaConstants,
    private var clientLogger: ClientLogger,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val popupModel: PopupModel,
    private val googleSignInAccountProvider: GoogleSignInAccountProvider
) {
    @Inject
    constructor(
        sharedPreferencesModel: SharedPreferencesModel,
        loginToken: LoginToken,
        neevaConstants: NeevaConstants,
        clientLogger: ClientLogger,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        popupModel: PopupModel
    ) : this(
        sharedPreferencesModel = sharedPreferencesModel,
        loginToken = loginToken,
        neevaConstants = neevaConstants,
        clientLogger = clientLogger,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        popupModel = popupModel,
        googleSignInAccountProvider = GoogleSignInAccountProvider {
            GoogleSignIn.getSignedInAccountFromIntent(it)
        }
    )

    companion object {
        private const val PREVIEW_MODE_COUNT_THRESHOLD = 10
        private const val SERVER_CLIENT_ID =
            "892902198757-84tm1f14ne0pa6n3dmeehgeo5mk4mhl9.apps.googleusercontent.com"
        private const val AUTH_PATH_GOOGLE_LOGIN = "login-mobile"
        private const val AUTH_PATH_DEFAULT_LOGIN = "login"

        const val TAG = "FirstRunModel"

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
    }

    private lateinit var googleSignInClient: GoogleSignInClient

    /** Holds the [LaunchLoginIntentParams] for the latest login */
    private val intentParamFlow = MutableStateFlow<LaunchLoginIntentParams?>(null)

    private fun authUri(
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
        SharedPrefFolder.FirstRun.didShowAdBlockOnboarding.set(
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

    fun logEvent(interaction: LogConfig.Interaction) {
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

    private fun launchCustomTabsLoginIntent(
        context: Context,
        provider: NeevaUser.SSOProvider,
        signup: Boolean,
        emailProvided: String?,
        passwordProvided: String? = null
    ) {
        if (signup && provider == NeevaUser.SSOProvider.OKTA &&
            emailProvided != null &&
            passwordProvided != null
        ) {
            coroutineScope.launch(dispatchers.io) {
                OktaSignUp.createOktaAccount(
                    activityContext = context,
                    popupModel = popupModel,
                    neevaConstants = neevaConstants,
                    emailProvided = emailProvided,
                    passwordProvided = passwordProvided
                )
            }
            return
        }

        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .intent
        intent.setPackage(
            CustomTabsClient.getPackageName(
                context, listOf("com.android.chrome")
            )
        )
        intent.data = authUri(
            signup,
            provider,
            emailProvided ?: ""
        )
        context.startActivity(intent)
    }

    /**
     * Executes [onSuccess] if we can extract a valid auth uri from the [ActivityResult] and
     * falls back to custom tabs if we can not.
     */
    fun handleLoginActivityResult(
        context: Context,
        result: ActivityResult,
        onSuccess: (Uri) -> Unit
    ) {
        extractLoginUri(result)
            ?.let { onSuccess(it) }
            ?: run {
                openInCustomTabs(
                    context = context,
                    uri = authUri(
                        signup = intentParamFlow.value?.signup ?: false,
                        provider = intentParamFlow.value?.provider ?: NeevaUser.SSOProvider.GOOGLE,
                        loginHint = intentParamFlow.value?.emailProvided ?: ""
                    )
                )
            }
    }

    private fun extractLoginUri(result: ActivityResult): Uri? {
        val data = result.takeIf { it.resultCode == Activity.RESULT_OK }?.data ?: run {
            Log.e(TAG, "ActivityResult was not successful: ${result.resultCode}")
            return null
        }

        try {
            val account = googleSignInAccountProvider.getGoogleSignInAccount(data)
            val idToken = account.result.idToken ?: return null
            val authCode = account.result.serverAuthCode ?: return null

            return authUri(
                signup = intentParamFlow.value?.signup ?: false,
                provider = intentParamFlow.value?.provider ?: NeevaUser.SSOProvider.GOOGLE,
                identityToken = idToken,
                authorizationCode = authCode
            )
        } catch (e: ApiException) {
            Log.e(TAG, "Failed to extract signed in account from intent", e)
            return null
        }
    }

    fun getLaunchLoginIntent(
        context: Context,
    ): (LaunchLoginIntentParams) -> Unit {
        return { launchLoginIntentParams ->
            intentParamFlow.value = launchLoginIntentParams

            when (launchLoginIntentParams.provider) {
                NeevaUser.SSOProvider.MICROSOFT -> {
                    launchCustomTabsLoginIntent(
                        context = context,
                        provider = NeevaUser.SSOProvider.MICROSOFT,
                        signup = launchLoginIntentParams.signup,
                        emailProvided = launchLoginIntentParams.emailProvided
                    )
                    logEvent(LogConfig.Interaction.AUTH_SIGN_UP_WITH_MICROSOFT)
                }

                NeevaUser.SSOProvider.GOOGLE -> {
                    launchCustomTabsLoginIntent(
                        context = context,
                        provider = NeevaUser.SSOProvider.GOOGLE,
                        signup = launchLoginIntentParams.signup,
                        emailProvided = launchLoginIntentParams.emailProvided
                    )
                    logEvent(LogConfig.Interaction.AUTH_SIGN_UP_WITH_GOOGLE)
                }

                NeevaUser.SSOProvider.OKTA -> {
                    launchCustomTabsLoginIntent(
                        context = context,
                        provider = NeevaUser.SSOProvider.OKTA,
                        signup = launchLoginIntentParams.signup,
                        emailProvided = launchLoginIntentParams.emailProvided,
                        passwordProvided = launchLoginIntentParams.passwordProvided
                    )
                }

                else -> { }
            }
        }
    }
}

data class LaunchLoginIntentParams(
    val provider: NeevaUser.SSOProvider,
    val signup: Boolean,
    val emailProvided: String? = null,
    val passwordProvided: String? = null,
    val resultLauncher: ActivityResultLauncher<Intent>
)
