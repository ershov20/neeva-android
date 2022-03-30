package com.neeva.app.settings

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.R
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.settings.clearBrowsing.TimeClearingOption
import com.neeva.app.settings.setDefaultAndroidBrowser.FakeSetDefaultAndroidBrowserManager
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData
import java.util.Date

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
    fun getOnClickMap(): Map<Int, (() -> Unit)?>
    //endregion

    //region Profile Settings
    fun isSignedOut(): Boolean
    fun getNeevaUserData(): NeevaUserData
    fun signOut()
    //endregion

    //region Clear Browsing Data
    fun clearBrowsingData(
        clearingOptions: Map<String, Boolean>,
        timeClearingOption: TimeClearingOption
    )
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
    private val onSignOut: () -> Unit,
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

    override fun getOnClickMap(): Map<Int, (() -> Unit)?> {
        val resultMap = mutableMapOf<Int, (() -> Unit)?>()
        resultMap.putAll(getNavOnClickMap())
        resultMap.putAll(getButtonClicks())
        return resultMap
    }

    private fun getNavOnClickMap(): Map<Int, (() -> Unit)?> {
        val navMap = mutableMapOf<Int, (() -> Unit)?>(
            R.string.settings_sign_in_to_join_neeva to { appNavModel.showProfileSettings() },
            R.string.settings_clear_browsing_data to { appNavModel.showClearBrowsingSettings() },
            R.string.settings_default_browser to { appNavModel.showDefaultBrowserSettings() },
            R.string.settings_debug_local_feature_flags to {
                appNavModel.showLocalFeatureFlagsPane()
            }
        )
        if (isSignedOut()) {
            navMap[R.string.settings_sign_in_to_join_neeva] = { appNavModel.showFirstRun() }
        }
        return navMap
    }

    private fun getButtonClicks(): Map<Int, (() -> Unit)?> {
        return mutableMapOf(
            R.string.settings_sign_out to { signOut() },
            R.string.settings_debug_open_50_tabs to { appNavModel.debugOpenManyTabs() }
        )
    }

    override fun isSignedOut(): Boolean {
        return neevaUser.isSignedOut()
    }

    override fun getNeevaUserData(): NeevaUserData {
        return neevaUser.data
    }

    override fun signOut() {
        onSignOut()
        onBackPressed()
    }

    override fun clearBrowsingData(
        clearingOptions: Map<String, Boolean>,
        timeClearingOption: TimeClearingOption
    ) {
        val toMillis = Date().time
        val fromMillis = when (timeClearingOption) {
            TimeClearingOption.LAST_HOUR -> toMillis - DateUtils.HOUR_IN_MILLIS
            TimeClearingOption.TODAY -> toMillis - DateUtils.DAY_IN_MILLIS
            TimeClearingOption.TODAY_AND_YESTERDAY -> toMillis - DateUtils.DAY_IN_MILLIS * 2
            TimeClearingOption.EVERYTHING -> 0L
        }
        webLayerModel.clearBrowsingData(clearingOptions, fromMillis, toMillis)
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
val mockSettingsViewModel by lazy {
    object : SettingsViewModel {
        override fun onBackPressed() {}

        override fun getTogglePreferenceSetter(key: String?): ((Boolean) -> Unit) {
            return { }
        }

        override fun getToggleState(key: String?): MutableState<Boolean> {
            return mutableStateOf(false)
        }

        override fun openUrl(uri: Uri, openViaIntent: Boolean) {}

        override fun getOnClickMap(): Map<Int, (() -> Unit)?> {
            return mapOf(
                R.string.settings_sign_in_to_join_neeva to { },
                R.string.settings_clear_browsing_data to { },
                R.string.settings_default_browser to { },
                R.string.settings_debug_local_feature_flags to { }
            )
        }

        override fun isSignedOut(): Boolean { return false }

        override fun getNeevaUserData(): NeevaUserData {
            return NeevaUserData(
                displayName = "Jehan Kobe Chang",
                email = "kobec@neeva.co",
                pictureURI = Uri.parse("https://c.neevacdn.net/image/fetch/s")
            )
        }

        override fun signOut() {}

        override fun clearBrowsingData(
            clearingOptions: Map<String, Boolean>,
            timeClearingOption: TimeClearingOption
        ) {}

        override fun getSetDefaultAndroidBrowserManager(): SetDefaultAndroidBrowserManager {
            return FakeSetDefaultAndroidBrowserManager()
        }

        override fun openAndroidDefaultBrowserSettings() { }

        override fun isDebugMode(): Boolean { return true }
    }
}
