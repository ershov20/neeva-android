package com.neeva.app.userdata

import android.content.Intent
import android.net.Uri
import com.neeva.app.NeevaConstants
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel

/**
 * Singleton that provides and saves Neeva user identity token to SharedPrefs.
 */
class NeevaUserToken(val sharedPreferencesModel: SharedPreferencesModel) {
    var cachedToken: String = ""
    init {
        cachedToken = getTokenFromSharedPref()
    }
    companion object {
        internal const val KEY_TOKEN = "TOKEN"

        fun extractAuthTokenFromIntent(intent: Intent?): String? {
            val dataString = intent?.dataString ?: return null
            val dataUri = Uri.parse(dataString)
            // TODO(kobec): waiting on the backend to fix this https://github.com/neevaco/neeva/issues/63900
            if (!dataString.startsWith("neeva:://login") &&
                !(dataUri.scheme == "neeva" && dataUri.host == "login")
            ) {
                return null
            }

            // The URI is not hierarchical so none of the nicer getQueryForKey calls work.
            val token = dataUri.query?.substringAfter("sessionKey=")
            return if (token.isNullOrEmpty()) null else token
        }
    }

    fun loginCookieString(): String {
        return "${NeevaConstants.loginCookie}=${getToken()}"
    }

    fun getToken(): String {
        return cachedToken
    }

    fun getTokenFromSharedPref(): String {
        return sharedPreferencesModel.getValue(SharedPrefFolder.USER, KEY_TOKEN, "")
    }

    fun setToken(token: String) {
        cachedToken = token
        return sharedPreferencesModel.setValue(SharedPrefFolder.USER, KEY_TOKEN, token)
    }

    fun removeToken() {
        cachedToken = ""
        sharedPreferencesModel.removeValue(SharedPrefFolder.USER, KEY_TOKEN)
    }
}
