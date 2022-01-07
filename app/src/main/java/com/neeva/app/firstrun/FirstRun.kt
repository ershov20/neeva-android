package com.neeva.app.firstrun

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.R
import com.neeva.app.User
import com.neeva.app.widgets.BrandedTextButton

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FirstRunContainer(
    appNavModel: AppNavModel
) {
    val state: AppNavState by appNavModel.state.collectAsState()
    val activityContext = LocalContext.current

    AnimatedVisibility(
        visible = state == AppNavState.FIRST_RUN,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.primary)
        ) {
            Column(Modifier.wrapContentSize().align(Alignment.Center)) {
                BrandedTextButton(enabled = true, stringResID = R.string.sign_in_with_google) {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(activityContext, FirstRun.authUri)
                }
            }
        }
    }
}

object FirstRun {
    private const val FIRST_RUN_PREFS_FOLDER_NAME = "FIRST_RUN_PREFERENCES"
    private const val FIRST_RUN_DONE_KEY = "HAS_FINISHED_FIRST_RUN"

    val authUri: Uri = Uri.Builder()
        .scheme("https")
        .authority("neeva.com")
        .path("login")
        .appendQueryParameter("provider", "neeva.co/auth/oauth2/authenticators/google")
        .appendQueryParameter("finalPath", "/")
        .appendQueryParameter("signup", "true")
        .appendQueryParameter("ignoreCountryCode", "true")
        .appendQueryParameter("loginCallbackType", "ios")
        .build()

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(FIRST_RUN_PREFS_FOLDER_NAME, MODE_PRIVATE)
    }

    fun shouldShowFirstRun(context: Context): Boolean {
        return User.getToken(context).isNullOrEmpty() &&
            !getSharedPreferences(context).getBoolean(FIRST_RUN_DONE_KEY, false)
    }

    fun firstRunDone(context: Context) {
        getSharedPreferences(context)
            .edit()
            .putBoolean(FIRST_RUN_DONE_KEY, true)
            .apply()
    }
}
