package com.neeva.app.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.BuildConfig
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel

/**
 * A data model for getting any SettingsToggle MutableState.
 * Used to get toggle states in SettingsViewModel.
 * FEATURE FLAGGING: used in any @Composable or anywhere else to get if a Feature Flag is enabled.
 *
 * This includes:
 *
 *    - Holding all toggle MutableStates (which are based on their SharedPref values)
 *    - Being a wrapper class for Settings-SharedPreferences
 *    - Holding DEBUG-mode-only flags as MutableStates
 */
class SettingsDataModel(val sharedPreferencesModel: SharedPreferencesModel) {
    private val toggleMap = mutableMapOf<String, MutableState<Boolean>>()
    val isDebugMode = BuildConfig.DEBUG

    init {
        SettingsToggle.values().forEach {
            toggleMap[it.key] = mutableStateOf(getSharedPrefValue(it.key, it.defaultValue))
        }
        LocalDebugFlags.values().forEach {
            toggleMap[it.key] = mutableStateOf(getSharedPrefValue(it.key, it.defaultValue, true))
        }
    }

    private fun getSharedPrefValue(
        key: String,
        default: Boolean,
        isDebugFlag: Boolean = false
    ): Boolean {
        if (isDebugFlag && !isDebugMode) {
            return false
        }
        return sharedPreferencesModel.getBoolean(SharedPrefFolder.SETTINGS, key, default)
    }

    private fun setSharedPrefValue(key: String, newValue: Boolean) {
        sharedPreferencesModel.setValue(SharedPrefFolder.SETTINGS, key, newValue)
    }

    fun getTogglePreferenceSetter(togglePreferenceKey: String?): (Boolean) -> Unit {
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
