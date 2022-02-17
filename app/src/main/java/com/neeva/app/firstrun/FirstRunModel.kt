package com.neeva.app.firstrun

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.compositionLocalOf
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserToken
import javax.inject.Inject

class FirstRunModel @Inject constructor(
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val neevaUserToken: NeevaUserToken
) {
    companion object {
        private const val FIRST_RUN_DONE_KEY = "HAS_FINISHED_FIRST_RUN"
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
                .getBoolean(SharedPrefFolder.FIRST_RUN, FIRST_RUN_DONE_KEY, false)
    }

    fun firstRunDone() {
        sharedPreferencesModel.setValue(SharedPrefFolder.FIRST_RUN, FIRST_RUN_DONE_KEY, true)
    }

    fun launchLoginIntent(
        activityContext: Context,
        provider: NeevaUser.SSOProvider,
        signup: Boolean,
        emailProvided: String
    ) {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .intent
        intent.setPackage(
            CustomTabsClient.getPackageName(
                activityContext, listOf("com.android.chrome")
            )
        )
        intent.data = authUri(
            signup,
            provider,
            emailProvided
        )
        activityContext.startActivity(intent)
    }
}

val LocalFirstRunModel = compositionLocalOf<FirstRunModel> { error("No value set") }
