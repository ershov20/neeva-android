package com.neeva.app

import android.content.Context

object User {
    private const val KEY_TOKEN = "TOKEN"
    private const val LOGIN_INFO_FOLDER_NAME = "UserLoginInfo"

    private fun preferences(context: Context) =
        context.getSharedPreferences(LOGIN_INFO_FOLDER_NAME, Context.MODE_PRIVATE)

    fun getToken(context: Context): String? {
        return preferences(context).getString(KEY_TOKEN, null)
    }

    fun setToken(context: Context, token: String) {
        preferences(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun removeToken(context: Context) {
        preferences(context).edit().remove(KEY_TOKEN).apply()
    }
}
