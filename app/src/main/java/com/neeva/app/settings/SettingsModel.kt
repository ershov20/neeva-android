package com.neeva.app.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class SettingsModel(application: Application) {
    private val SETTINGS_PREFS_FOLDER_NAME = "SETTINGS_PREFERENCES"

    val sharedPreferences: SharedPreferences = application
        .getSharedPreferences(SETTINGS_PREFS_FOLDER_NAME, Context.MODE_PRIVATE)

    val toggleMap = mutableMapOf<String, MutableState<Boolean>>()

    init {
        SettingsToggle.values().forEach {
            toggleMap[it.key] = mutableStateOf(getSharedPrefValue(it.key, it.defaultValue))
        }
    }

    fun getSharedPrefValue(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    fun setSharedPrefValue(key: String, newValue: Boolean) {
        sharedPreferences
            .edit()
            .putBoolean(key, newValue)
            .apply()
    }

    fun getTogglePreferenceSetter(togglePreferenceKey: String?): ((Boolean) -> Unit)? {
        return { newValue ->
            val toggleState = toggleMap[togglePreferenceKey]
            toggleState?.value = newValue
            togglePreferenceKey?.let { setSharedPrefValue(it, newValue) }
        }
    }

    fun getToggleState(toggleKeyName: String?): MutableState<Boolean>? {
        return toggleKeyName?.let { toggleMap[toggleKeyName] }
    }
}
