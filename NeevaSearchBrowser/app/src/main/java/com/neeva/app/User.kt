package com.neeva.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object User {
    private const val KEY_TOKEN = "TOKEN"
    private const val LOGIN_INFO_FOLDER_NAME = "UserLoginInfo"
    private fun preferences() = NeevaBrowser.context.getSharedPreferences(
            LOGIN_INFO_FOLDER_NAME, Context.MODE_PRIVATE)

    fun getToken(): String? {
        return preferences().getString(KEY_TOKEN, null)
    }

    fun setToken(token: String) {
        preferences().edit().apply {
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    fun removeToken() {
        preferences().edit().apply {
            remove(KEY_TOKEN)
            apply()
        }
    }
}