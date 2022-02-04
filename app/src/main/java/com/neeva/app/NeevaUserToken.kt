package com.neeva.app

import android.content.Intent
import android.net.Uri
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel

/**
 * Provides Neeva user identity token. Good for checking if the user is logged in.
 */
class NeevaUserToken(val sharedPreferencesModel: SharedPreferencesModel) {
    companion object {
        internal const val KEY_TOKEN = "TOKEN"

        fun extractAuthTokenFromIntent(intent: Intent?): String? {
            val dataString = intent?.dataString ?: return null

            val dataUri = Uri.parse(dataString)
            if (dataUri.scheme != "neeva" || dataUri.host != "login") return null

            // The URI is not hierarchical so none of the nicer getQueryForKey calls work.
            val token = dataUri.query?.substringAfter("sessionKey=")
            return if (token.isNullOrEmpty()) null else token
        }
    }

    fun loginCookieString(): String {
        return "${NeevaConstants.loginCookie}=${getToken()}"
    }

    fun getToken(): String? {
        val result = sharedPreferencesModel.getString(SharedPrefFolder.USER, KEY_TOKEN, "")
        return if (result == "") {
            null
        } else {
            result
        }
    }

    fun setToken(token: String) {
        return sharedPreferencesModel.setValue(SharedPrefFolder.USER, KEY_TOKEN, token)
    }

    fun removeToken() {
        sharedPreferencesModel.removeValue(SharedPrefFolder.USER, KEY_TOKEN)
    }
}
