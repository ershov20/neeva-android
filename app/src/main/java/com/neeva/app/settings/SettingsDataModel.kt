package com.neeva.app.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.BuildConfig
import com.neeva.app.settings.clearBrowsing.TimeClearingOptionsConstants
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel

/**
 * A data model for getting any Settings-related state (SettingsToggle or SelectedTimeClearingOption).
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
    val selectedTimeClearingOption = mutableStateOf(
        getSharedPrefValue(TimeClearingOptionsConstants.sharedPrefKey, 0)
    )

    init {
        SettingsToggle.values().forEach {
            toggleMap[it.key] = mutableStateOf(getToggleSharedPrefValue(it.key, it.defaultValue))
        }
        LocalDebugFlags.values().forEach {
            toggleMap[it.key] = mutableStateOf(
                getToggleSharedPrefValue(it.key, it.defaultValue, true)
            )
        }
    }

    private fun getToggleSharedPrefValue(
        key: String,
        default: Boolean,
        isDebugFlag: Boolean = false
    ): Boolean {
        if (isDebugFlag && !isDebugMode) {
            return false
        }
        return sharedPreferencesModel.getValue(SharedPrefFolder.SETTINGS, key, default)
    }

    private fun <T> getSharedPrefValue(key: String, defaultValue: T): T {
        return sharedPreferencesModel.getValue(SharedPrefFolder.SETTINGS, key, defaultValue)
    }

    private fun setSharedPrefValue(key: String, newValue: Any) {
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

    fun getTimeClearingOption(): MutableState<Int> {
        return selectedTimeClearingOption
    }

    fun saveSelectedTimeClearingOption(index: Int) {
        setSharedPrefValue(TimeClearingOptionsConstants.sharedPrefKey, index)
    }
}
