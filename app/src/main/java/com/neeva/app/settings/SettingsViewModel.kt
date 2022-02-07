package com.neeva.app.settings

import android.net.Uri
import androidx.compose.runtime.MutableState
import com.neeva.app.AppNavModel
import com.neeva.app.history.HistoryManager

/**
 * An interface handling all Settings-related controller logic.
 *
 * Uses SettingsDataModel to provide UI State.
 */
interface SettingsViewModel {
    fun onBackPressed()
    fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit)?
    fun getToggleState(key: String?): MutableState<Boolean>?
    fun openUrl(uri: Uri)
    fun showFirstRun()
    fun showClearBrowsingSettings()
    fun showProfileSettings()
    fun clearAllHistory()
    fun isSignedOut(): Boolean
}

class SettingsViewModelImpl(
    val appNavModel: AppNavModel,
    val settingsDataModel: SettingsDataModel,
    val historyManager: HistoryManager
) : SettingsViewModel {

    override fun onBackPressed() {
        appNavModel.popBackStack()
    }

    override fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit)? {
        return settingsDataModel.getTogglePreferenceSetter(key)
    }

    override fun getToggleState(key: String?): MutableState<Boolean>? {
        return settingsDataModel.getToggleState(key)
    }

    override fun openUrl(uri: Uri) {
        appNavModel.openUrl(uri)
    }

    override fun showFirstRun() {
        appNavModel.showFirstRun()
    }

    override fun showClearBrowsingSettings() {
        appNavModel.showClearBrowsingSettings()
    }

    override fun showProfileSettings() {
        appNavModel.showProfileSettings()
    }

    override fun clearAllHistory() {
        historyManager.clearAllHistory()
    }

    override fun isSignedOut(): Boolean {
        return settingsDataModel.isSignedOut()
    }
}

internal fun getFakeSettingsViewModel(): SettingsViewModel {
    return object : SettingsViewModel {
        override fun onBackPressed() {}

        override fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit)? { return null }

        override fun getToggleState(key: String?): MutableState<Boolean>? { return null }

        override fun openUrl(uri: Uri) {}

        override fun showFirstRun() {}

        override fun showClearBrowsingSettings() {}

        override fun showProfileSettings() {}

        override fun clearAllHistory() {}

        override fun isSignedOut(): Boolean { return false }
    }
}
