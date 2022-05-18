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
    private val sharedPreferencesMap = mutableMapOf<SharedPrefFolder, SharedPreferences>().apply {
        SharedPrefFolder.values().forEach {
            put(it, context.getSharedPreferences(it.name, Context.MODE_PRIVATE))
        }
    }

    fun <T> getValue(folder: SharedPrefFolder, key: String, defaultValue: T): T {
        val sharedPrefs = sharedPreferencesMap[folder]
        val returnValue: Any? = when (defaultValue) {
            is Boolean -> sharedPrefs?.getBoolean(key, defaultValue)
            is String -> sharedPrefs?.getString(key, defaultValue)
            is Int -> sharedPrefs?.getInt(key, defaultValue)
            else -> defaultValue
        }
        return (returnValue as? T) ?: defaultValue
    }

    fun setValue(folder: SharedPrefFolder, key: String, value: Any) {
        val editor = sharedPreferencesMap[folder]?.edit()
        if (editor != null) {
            val putLambda = when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                else -> null
            }
            putLambda?.commit()
        }
    }

    fun removeValue(folder: SharedPrefFolder, key: String) {
        sharedPreferencesMap[folder]
            ?.edit()
            ?.remove(key)
            ?.commit()
    }
}

enum class SharedPrefFolder {
    SETTINGS,
    USER,
    FIRST_RUN,
    ZERO_QUERY,
    HISTORY_UI,
    APP,
    COOKIE_CUTTER,
    WEBLAYER,
    SPACES
}
