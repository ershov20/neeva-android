package com.neeva.app

import android.content.Context
import android.content.Intent
import android.net.Uri

object User {
    private const val KEY_TOKEN = "TOKEN"
    private const val LOGIN_INFO_FOLDER_NAME = "UserLoginInfo"

    private fun preferences(context: Context) =
        context.getSharedPreferences(LOGIN_INFO_FOLDER_NAME, Context.MODE_PRIVATE)

    fun loginCookieString(context: Context) =
        "${NeevaConstants.loginCookie}=${User.getToken(context)}"

    fun getToken(context: Context): String? {
        return preferences(context).getString(KEY_TOKEN, null)
    }

    fun setToken(context: Context, token: String) {
        preferences(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun removeToken(context: Context) {
        preferences(context).edit().remove(KEY_TOKEN).apply()
    }

    fun extractAuthTokenFromIntent(intent: Intent?) : String? {
        val dataUri = Uri.parse(intent?.dataString) ?: return null
        if (dataUri.scheme != "neeva" && dataUri.host != "login") return null
        // The URI is not hierarchical so none of the nicer getQueryForKey calls work.
        val token = dataUri.query?.substringAfter("sessionKey=")
        return if (token.isNullOrEmpty()) null else token
    }
}
