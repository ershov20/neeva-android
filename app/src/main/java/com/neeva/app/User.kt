package com.neeva.app

import android.content.Context

object User {
    private const val KEY_TOKEN = "TOKEN"
    private const val LOGIN_INFO_FOLDER_NAME = "UserLoginInfo"

    private fun preferences() =
        NeevaBrowser.context.getSharedPreferences(LOGIN_INFO_FOLDER_NAME, Context.MODE_PRIVATE)

    fun getToken(): String? {
        return preferences().getString(KEY_TOKEN, null)
    }

    fun setToken(token: String) {
        preferences().edit().putString(KEY_TOKEN, token).apply()
    }

    fun removeToken() {
        preferences().edit().remove(KEY_TOKEN).apply()
    }
}