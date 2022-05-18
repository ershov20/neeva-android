package com.neeva.app.firstrun

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.compositionLocalOf
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserToken
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FirstRunModel @Inject constructor(
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val neevaUserToken: NeevaUserToken,
    private val neevaConstants: NeevaConstants,
    private var clientLogger: ClientLogger,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val snackbarModel: SnackbarModel
) {
    companion object {
        private const val FIRST_RUN_DONE_KEY = "HAS_FINISHED_FIRST_RUN"
        private const val SHOULD_LOG_FIRST_LOGIN_KEY = "SHOULD_LOG_FIRST_LOGIN"

        fun firstRunDone(sharedPreferencesModel: SharedPreferencesModel) {
            sharedPreferencesModel.setValue(
                SharedPrefFolder.FIRST_RUN, FIRST_RUN_DONE_KEY, true
            )
            sharedPreferencesModel.setValue(
                SharedPrefFolder.FIRST_RUN, SHOULD_LOG_FIRST_LOGIN_KEY, true
            )
        }
    }

    private fun authUri(
        signup: Boolean,
        provider: NeevaUser.SSOProvider,
        loginHint: String = ""
    ): Uri {
        val builder = Uri.Builder()
            .scheme("https")
            .authority("neeva.com")
            .path("login")
            .appendQueryParameter("provider", provider.url)
            .appendQueryParameter("finalPath", provider.finalPath)
            .appendQueryParameter("signup", signup.toString())
            .appendQueryParameter("ignoreCountryCode", "true")
            .appendQueryParameter("loginCallbackType", "ios")
        return when (provider) {
            NeevaUser.SSOProvider.OKTA ->
                builder
                    .appendQueryParameter("loginHint", loginHint)
                    .build()
            else -> builder.build()
        }
    }

    fun shouldShowFirstRun(): Boolean {
        return neevaUserToken.getToken().isEmpty() &&
            !sharedPreferencesModel
                .getValue(SharedPrefFolder.FIRST_RUN, FIRST_RUN_DONE_KEY, false)
    }

    fun firstRunDone() {
        firstRunDone(sharedPreferencesModel)
    }

    fun shouldLogFirstLogin(): Boolean {
        return sharedPreferencesModel.getValue(
            SharedPrefFolder.FIRST_RUN, SHOULD_LOG_FIRST_LOGIN_KEY, false
        )
    }

    fun setShouldLogFirstLogin(value: Boolean) {
        sharedPreferencesModel.setValue(
            SharedPrefFolder.FIRST_RUN, SHOULD_LOG_FIRST_LOGIN_KEY, value
        )
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

    fun openInCustomTabs(context: Context): (Uri) -> Unit {
        return { uri ->
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .intent
            intent.setPackage(
                CustomTabsClient.getPackageName(context, listOf("com.android.chrome"))
            )
            intent.data = uri
            context.startActivity(intent)
        }
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
                    snackbarModel = snackbarModel,
                    neevaUserToken = neevaUserToken,
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

    fun getLaunchLoginIntent(
        context: Context,
    ): (LaunchLoginIntentParams) -> Unit {
        return { launchLoginIntentParams ->
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
    val passwordProvided: String? = null
)

val LocalFirstRunModel = compositionLocalOf<FirstRunModel> { error("No value set") }
