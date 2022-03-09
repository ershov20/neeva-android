package com.neeva.app.settings

import android.net.Uri
import androidx.compose.runtime.MutableState
import com.neeva.app.R
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.settings.setDefaultAndroidBrowser.FakeSetDefaultAndroidBrowserManager
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData

/**
 * An interface handling all Settings-related controller logic.
 *
 * Uses SettingsDataModel to provide UI State.
 */
interface SettingsViewModel {
    //region For General Settings Pane UI behavior
    fun onBackPressed()
    fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit)?
    fun getToggleState(key: String?): MutableState<Boolean>?
    fun openUrl(uri: Uri, openViaIntent: Boolean)
    //endregion

    //region Main Settings
    fun getMainSettingsNavigation(): Map<Int, (() -> Unit)?>
    //endregion

    //region Profile Settings
    fun isSignedOut(): Boolean
    fun getNeevaUserData(): NeevaUserData
    fun signOut()
    //endregion

    //region Clear Browsing Data
    fun clearBrowsingData(clearingOptions: Map<String, Boolean>)
    //endregion

    //region Set Default Android Browser
    fun getSetDefaultAndroidBrowserManager(): SetDefaultAndroidBrowserManager

    // Meant for system images lower than Android Q
    fun openAndroidDefaultBrowserSettings()
    //endregion

    //region Debug Settings
    fun isDebugMode(): Boolean
    //
}

class SettingsViewModelImpl(
    private val appNavModel: AppNavModel,
    private val settingsDataModel: SettingsDataModel,
    private val neevaUser: NeevaUser,
    private val webLayerModel: WebLayerModel,
    private val setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager
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

    override fun getMainSettingsNavigation(): Map<Int, (() -> Unit)?> {
        val navMap = mutableMapOf<Int, (() -> Unit)?>(
            R.string.settings_sign_in_to_join_neeva to appNavModel::showProfileSettings,
            R.string.settings_clear_browsing_data to appNavModel::showClearBrowsingSettings,
            R.string.settings_default_browser to appNavModel::showDefaultBrowserSettings,
            R.string.settings_debug_local_feature_flags to appNavModel::showLocalFeatureFlagsPane
        )
        if (isSignedOut()) {
            navMap[R.string.settings_sign_in_to_join_neeva] = appNavModel::showFirstRun
        }
        return navMap
    }

    override fun isSignedOut(): Boolean {
        return neevaUser.isSignedOut()
    }

    override fun getNeevaUserData(): NeevaUserData {
        return neevaUser.data
    }

    override fun signOut() {
        neevaUser.signOut(webLayerModel)
        onBackPressed()
    }

    override fun clearBrowsingData(clearingOptions: Map<String, Boolean>) {
        webLayerModel.clearBrowsingData(clearingOptions)
    }

    override fun getSetDefaultAndroidBrowserManager(): SetDefaultAndroidBrowserManager {
        return setDefaultAndroidBrowserManager
    }

    override fun openAndroidDefaultBrowserSettings() {
        appNavModel.openAndroidDefaultBrowserSettings()
    }

    override fun isDebugMode(): Boolean {
        return settingsDataModel.isDebugMode
    }
}

/** For Preview testing. */
internal fun getFakeSettingsViewModel(): SettingsViewModel {
    return object : SettingsViewModel {
        override fun onBackPressed() {}

        override fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit)? { return null }

        override fun getToggleState(key: String?): MutableState<Boolean>? { return null }

        override fun openUrl(uri: Uri, openViaIntent: Boolean) {}

        override fun getMainSettingsNavigation(): Map<Int, (() -> Unit)?> { return mapOf() }

        override fun isSignedOut(): Boolean { return false }

        override fun getNeevaUserData(): NeevaUserData {
            return NeevaUserData(
                displayName = "Jehan Kobe Chang",
                email = "kobec@neeva.co",
                pictureURL = Uri.parse("https://c.neevacdn.net/image/fetch/s")
            )
        }

        override fun signOut() {}

        override fun clearBrowsingData(clearingOptions: Map<String, Boolean>) {}

        override fun getSetDefaultAndroidBrowserManager(): SetDefaultAndroidBrowserManager {
            return FakeSetDefaultAndroidBrowserManager()
        }

        override fun openAndroidDefaultBrowserSettings() { }

        override fun isDebugMode(): Boolean { return true }
    }
}
