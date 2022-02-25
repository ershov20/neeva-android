package com.neeva.app.settings

import android.net.Uri
import androidx.compose.runtime.MutableState
import com.neeva.app.R
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData

/**
 * An interface handling all Settings-related controller logic.
 *
 * Uses SettingsDataModel to provide UI State.
 */
interface SettingsViewModel {
    fun onBackPressed()
    fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit)?
    fun getToggleState(key: String?): MutableState<Boolean>?
    fun openUrl(uri: Uri, openViaIntent: Boolean)
    fun navigateToSubPane(settingsRowTitleId: Int, isSignedOut: Boolean): () -> Unit
    fun clearAllHistory()
    fun isSignedOut(): Boolean
    fun getNeevaUserData(): NeevaUserData
    fun openAndroidDefaultBrowserSettings()
}

class SettingsViewModelImpl(
    val appNavModel: AppNavModel,
    val settingsDataModel: SettingsDataModel,
    val historyManager: HistoryManager,
    val neevaUser: NeevaUser
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

    override fun openUrl(uri: Uri, openViaIntent: Boolean) {
        if (openViaIntent) {
            appNavModel.openUrlViaIntent(uri)
        } else {
            appNavModel.openUrl(uri)
        }
    }

    private val onClickMap = mapOf(
        R.string.settings_sign_in_to_join_neeva to
            appNavModel::showProfileSettings,
        R.string.settings_clear_browsing_data to
            appNavModel::showClearBrowsingSettings,
        R.string.settings_default_browser to
            appNavModel::showDefaultBrowserSettings
    )
    override fun navigateToSubPane(settingsRowTitleId: Int, isSignedOut: Boolean): () -> Unit {
        if (
            settingsRowTitleId == R.string.settings_sign_in_to_join_neeva &&
            isSignedOut
        ) {
            return appNavModel::showFirstRun
        }
        return onClickMap[settingsRowTitleId] ?: {}
    }

    override fun clearAllHistory() {
        historyManager.clearAllHistory()
    }

    override fun isSignedOut(): Boolean {
        return neevaUser.isSignedOut()
    }

    override fun getNeevaUserData(): NeevaUserData {
        return neevaUser.data
    }

    override fun openAndroidDefaultBrowserSettings() {
        appNavModel.openAndroidDefaultBrowserSettings()
    }
}

internal fun getFakeSettingsViewModel(): SettingsViewModel {
    return object : SettingsViewModel {
        override fun onBackPressed() {}

        override fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit)? { return null }

        override fun getToggleState(key: String?): MutableState<Boolean>? { return null }

        override fun openUrl(uri: Uri, openViaIntent: Boolean) {}

        override fun navigateToSubPane(settingsRowTitleId: Int, isSignedOut: Boolean): () -> Unit {
            return {}
        }

        override fun clearAllHistory() {}

        override fun isSignedOut(): Boolean { return false }

        override fun getNeevaUserData(): NeevaUserData {
            return NeevaUserData(
                displayName = "Jehan Kobe Chang",
                email = "kobec@neeva.co",
                pictureURL = Uri.parse("https://c.neevacdn.net/image/fetch/s")
            )
        }

        override fun openAndroidDefaultBrowserSettings() { }
    }
}
