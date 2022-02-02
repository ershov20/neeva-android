package com.neeva.app.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.NeevaUserToken
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.NeevaUser

/**
 * An interface used for anything Settings-related.
 *
 * This includes:
 *    - holding toggle state
 *    - triggering signing in/out
 *    - storing Settings-SharedPreferences
 */
class SettingsModel(
    val sharedPreferencesModel: SharedPreferencesModel,
    val neevaUserToken: NeevaUserToken
) {
    private val toggleMap = mutableMapOf<String, MutableState<Boolean>>()

    init {
        SettingsToggle.values().forEach {
            toggleMap[it.key] = mutableStateOf(getSharedPrefValue(it.key, it.defaultValue))
        }
    }

    private fun getSharedPrefValue(key: String, default: Boolean): Boolean {
        return sharedPreferencesModel.getBoolean(SharedPrefFolder.SETTINGS, key, default)
    }

    private fun setSharedPrefValue(key: String, newValue: Boolean) {
        sharedPreferencesModel.setValue(SharedPrefFolder.SETTINGS, key, newValue)
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

    fun signOut() {
        NeevaUser.shared = NeevaUser()
        neevaUserToken.removeToken()
        // TODO(kobec): load different settings preferences per user
    }

    fun isSignedIn(): Boolean {
        return !neevaUserToken.getToken().isNullOrEmpty() && NeevaUser.shared.id != null
    }
}
