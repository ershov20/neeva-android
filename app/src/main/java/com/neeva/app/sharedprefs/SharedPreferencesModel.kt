package com.neeva.app.sharedprefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Provides access to SharedPreferences for all features.
 *
 * Useful because:
 *      1) nobody needs to hold on to a Context anymore.
 *      2) SharedPref functions don't need to be rewritten.
 */
class SharedPreferencesModel(context: Context) {
    val sharedPreferencesMap: Map<SharedPrefFolder, SharedPreferences> =
        mapOf(
            SharedPrefFolder.SETTINGS to context.getSharedPreferences(
                SharedPrefFolder.SETTINGS.name, Context.MODE_PRIVATE
            ),
            SharedPrefFolder.USER to context.getSharedPreferences(
                SharedPrefFolder.USER.name, Context.MODE_PRIVATE
            ),
            SharedPrefFolder.FIRST_RUN to context.getSharedPreferences(
                SharedPrefFolder.FIRST_RUN.name, Context.MODE_PRIVATE
            ),
        )

    fun getString(folder: SharedPrefFolder, key: String, defaultValue: String): String {
        return sharedPreferencesMap[folder]?.getString(key, defaultValue) ?: defaultValue
    }

    fun getBoolean(folder: SharedPrefFolder, key: String, defaultValue: Boolean): Boolean {
        return sharedPreferencesMap[folder]?.getBoolean(key, defaultValue) ?: defaultValue
    }

    fun setValue(folder: SharedPrefFolder, key: String, value: Boolean) {
        sharedPreferencesMap[folder]
            ?.edit()
            ?.putBoolean(key, value)
            ?.apply()
    }

    fun setValue(folder: SharedPrefFolder, key: String, value: String) {
        sharedPreferencesMap[folder]
            ?.edit()
            ?.putString(key, value)
            ?.apply()
    }

    fun removeValue(folder: SharedPrefFolder, key: String) {
        sharedPreferencesMap[folder]
            ?.edit()
            ?.remove(key)
            ?.apply()
    }
}

enum class SharedPrefFolder(name: String) {
    SETTINGS("SETTINGS_PREFERENCES"),
    USER("UserLoginInfo"),
    FIRST_RUN("FIRST_RUN_PREFERENCES")
}
