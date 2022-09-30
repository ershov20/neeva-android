// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.R
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.ActivityCallbackProvider
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.archived.ArchivingOptionsDialog
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.settings.clearbrowsing.TimeClearingOption
import com.neeva.app.settings.defaultbrowser.FakeSetDefaultAndroidBrowserManager
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.UserInfo
import java.util.Date
import java.util.EnumSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * An interface handling all Settings-related controller logic.
 *
 * Uses SettingsDataModel to provide UI State.
 */
interface SettingsController {
    //region For General Settings Pane UI behavior
    fun onBackPressed()
    fun getTogglePreferenceSetter(settingsToggle: SettingsToggle): (Boolean) -> Unit
    fun getToggleState(settingsToggle: SettingsToggle): MutableState<Boolean>
    fun openUrl(uri: Uri, openViaIntent: Boolean)
    //endregion

    //region Main Settings
    fun getOnClickMap(): Map<Int, (() -> Unit)?>
    fun getOnDoubleClickMap(): Map<Int, (() -> Unit)?>
    fun getToggleChangedCallBackMap(): Map<String, (() -> Unit)?>
    //endregion

    //region Profile Settings
    fun isSignedOut(): Boolean
    fun getNeevaUserInfoFlow(): StateFlow<UserInfo?>
    fun signOut()
    //endregion

    //region Clear Browsing Data
    fun clearBrowsingData(
        clearingOptions: Map<SettingsToggle, Boolean>,
        timeClearingOption: TimeClearingOption
    )
    //endregion

    //region Cookie Cutter Data
    fun getContentFilterStrength(): ContentFilterModel.BlockingStrength
    fun setContentFilterStrength(strength: ContentFilterModel.BlockingStrength)
    fun getCookieNoticeSelection(): ContentFilterModel.CookieNoticeSelection
    fun setCookieNoticeSelection(selection: ContentFilterModel.CookieNoticeSelection)
    fun getCookieNoticePreferences(): Set<ContentFilterModel.CookieNoticeCookies>
    fun setCookieNoticePreferences(preferences: Set<ContentFilterModel.CookieNoticeCookies>)
    //endregion

    //region Set Default Android Browser
    fun getSetDefaultAndroidBrowserManager(): SetDefaultAndroidBrowserManager

    // Meant for system images lower than Android Q
    fun openAndroidDefaultBrowserSettings()
    //endregion

    //region Debug Settings
    fun isAdvancedSettingsAllowed(): Boolean
    //
}

class SettingsControllerImpl(
    private val appNavModel: AppNavModel,
    private val settingsDataModel: SettingsDataModel,
    private val neevaUser: NeevaUser,
    private val webLayerModel: WebLayerModel,
    private val onSignOut: () -> Unit,
    private val setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
    private val coroutineScope: CoroutineScope,
    private val popupModel: PopupModel,
    private val activityCallbackProvider: ActivityCallbackProvider,
    private val onTrackingProtectionUpdate: () -> Unit
) : SettingsController {
    override fun onBackPressed() {
        appNavModel.popBackStack()
    }

    override fun getTogglePreferenceSetter(settingsToggle: SettingsToggle): (Boolean) -> Unit {
        return { newValue ->
            settingsDataModel.getTogglePreferenceSetter(settingsToggle)(newValue)
            getToggleChangedCallBackMap()[settingsToggle.key]?.invoke()
        }
    }

    override fun getToggleState(settingsToggle: SettingsToggle): MutableState<Boolean> {
        return settingsDataModel.getToggleState(settingsToggle)
    }

    override fun openUrl(uri: Uri, openViaIntent: Boolean) {
        if (openViaIntent) {
            appNavModel.openUrlViaIntent(uri)
        } else {
            appNavModel.openUrlInNewTab(uri)
        }
    }

    override fun getOnClickMap(): Map<Int, (() -> Unit)?> {
        val resultMap = mutableMapOf<Int, (() -> Unit)?>()
        resultMap.putAll(getNavOnClickMap())
        resultMap.putAll(getButtonClicks())
        return resultMap
    }

    override fun getOnDoubleClickMap(): Map<Int, (() -> Unit)?> {
        return mapOf(
            R.string.settings_neeva_browser_version to { toggleIsAdvancedSettingsAllowed() }
        )
    }

    override fun getToggleChangedCallBackMap(): Map<String, (() -> Unit)?> {
        return mapOf(
            SettingsToggle.TRACKING_PROTECTION.key to {
                onTrackingProtectionUpdate()
            },
            SettingsToggle.DEBUG_USE_CUSTOM_DOMAIN.key to {
                popupModel.showSnackbar("Restart for app host changes to take effect")
            }
        )
    }

    private fun getNavOnClickMap(): Map<Int, (() -> Unit)?> {
        val navMap = mutableMapOf<Int, (() -> Unit)?>(
            R.string.settings_sign_in_to_join_neeva to { appNavModel.showProfileSettings() },
            R.string.settings_clear_browsing_data to { appNavModel.showClearBrowsingSettings() },
            R.string.settings_default_browser to { appNavModel.showDefaultBrowserSettings() },
            R.string.settings_debug_local_feature_flags to {
                appNavModel.showLocalFeatureFlagsPane()
            },
            R.string.archived_tabs_archive to {
                popupModel.showDialog {
                    ArchivingOptionsDialog(popupModel::removeDialog)
                }
            },
            R.string.content_filter to { appNavModel.showContentFilterSettings() },
            R.string.settings_licenses to { appNavModel.showLicenses() }
        )
        if (isSignedOut()) {
            navMap[R.string.settings_sign_in_to_join_neeva] = { appNavModel.showSignInFlow() }
        }
        return navMap
    }

    private fun getButtonClicks(): Map<Int, (() -> Unit)?> {
        return mapOf(
            R.string.settings_sign_out to { signOut() },
            R.string.settings_debug_custom_neeva_domain_current to { debugSetCustomBackend() },
            R.string.settings_debug_open_50_tabs to { debugOpenManyTabs() },
            R.string.settings_debug_open_500_tabs to { debugOpenManyTabs(500) },
            R.string.settings_debug_export_database to { debugExportDatabase() },
            R.string.settings_debug_import_database to { debugImportDatabase() }
        )
    }

    override fun isSignedOut(): Boolean {
        return neevaUser.isSignedOut()
    }

    override fun getNeevaUserInfoFlow(): StateFlow<UserInfo?> {
        return neevaUser.userInfoFlow
    }

    override fun signOut() {
        onSignOut()
        onBackPressed()
    }

    override fun clearBrowsingData(
        clearingOptions: Map<SettingsToggle, Boolean>,
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

    override fun isAdvancedSettingsAllowed(): Boolean {
        return settingsDataModel.getSettingsToggleValue(SettingsToggle.IS_ADVANCED_SETTINGS_ALLOWED)
    }

    override fun getContentFilterStrength(): ContentFilterModel.BlockingStrength {
        return settingsDataModel.getContentFilterStrength()
    }

    override fun setContentFilterStrength(strength: ContentFilterModel.BlockingStrength) {
        settingsDataModel.setContentFilterStrength(strength)
        onTrackingProtectionUpdate()
    }

    override fun getCookieNoticeSelection(): ContentFilterModel.CookieNoticeSelection {
        return if (getCookieNoticePreferences().isEmpty())
            ContentFilterModel.CookieNoticeSelection.DECLINE_COOKIES
        else ContentFilterModel.CookieNoticeSelection.ACCEPT_COOKIES
    }

    override fun setCookieNoticeSelection(selection: ContentFilterModel.CookieNoticeSelection) {
        when (selection) {
            ContentFilterModel.CookieNoticeSelection.ACCEPT_COOKIES -> {
                // only set cookie notices this way if we are switching from DECLINE_COOKIES
                // otherwise, keep the user's existing preferences and just show the preferences
                // pane
                if (
                    getCookieNoticeSelection() ===
                    ContentFilterModel.CookieNoticeSelection.DECLINE_COOKIES
                ) {
                    setCookieNoticePreferences(
                        EnumSet.allOf(ContentFilterModel.CookieNoticeCookies::class.java)
                    )
                }

                // prompt for more settings
                appNavModel.showCookiePreferences()
            }
            ContentFilterModel.CookieNoticeSelection.DECLINE_COOKIES -> {
                setCookieNoticePreferences(
                    EnumSet.noneOf(ContentFilterModel.CookieNoticeCookies::class.java)
                )
            }
        }
    }

    override fun getCookieNoticePreferences(): Set<ContentFilterModel.CookieNoticeCookies> {
        return settingsDataModel.getCookieNoticePreferences()
    }

    override fun setCookieNoticePreferences(
        preferences: Set<ContentFilterModel.CookieNoticeCookies>
    ) {
        settingsDataModel.setCookieNoticePreferences(preferences)
    }

    private fun toggleIsAdvancedSettingsAllowed() {
        settingsDataModel.toggleIsAdvancedSettingsAllowed()
    }

    private fun debugSetCustomBackend() {
        popupModel.showDialog {
            SetCustomBackendDialog(
                onShowSnackbar = { popupModel.showSnackbar(it) },
                onDismissRequested = popupModel::removeDialog
            )
        }
    }

    private fun debugOpenManyTabs(numTabs: Int = 50, msBetweenOpens: Long = 1000) {
        coroutineScope.launch {
            val possibleUrls = listOf(
                "https://en.wikipedia.org",
                "https://youtube.com",
                "https://amazon.com",
                "https://facebook.com",
                "https://twitter.com",
                "https://fandom.com",
                "https://pinterest.com",
                "https://imdb.com",
                "https://reddit.com",
                "https://yelp.com",
                "https://instagram.com",
                "https://ebay.com",
                "https://walmart.com",
                "https://craigslist.org",
                "https://healthline.com",
                "https://tripadvisor.com",
                "https://linkedin.com",
                "https://webmd.com",
                "https://netflix.com",
                "https://apple.com",
                "https://homedepot.com",
                "https://mail.yahoo.com",
                "https://cnn.com",
                "https://etsy.com",
                "https://google.com",
                "https://yahoo.com",
                "https://indeed.com",
                "https://target.com",
                "https://microsoft.com",
                "https://nytimes.com",
                "https://mayoclinic.org",
                "https://espn.com",
                "https://usps.com",
                "https://quizlet.com",
                "https://gamepedia.com",
                "https://lowes.com",
                "https://irs.gov",
                "https://nih.gov",
                "https://merriam-webster.com",
                "https://steampowered.com"
            )
            for (i in 0 until numTabs) {
                openUrl(uri = Uri.parse(possibleUrls[i % possibleUrls.size]), openViaIntent = false)
                delay(msBetweenOpens)
                if (i % 50 == 49) {
                    popupModel.showSnackbar("Opened ${i + 1}/$numTabs tabs")
                }
            }
        }
    }

    private fun debugExportDatabase() {
        activityCallbackProvider.get()?.exportHistoryDatabase()
    }

    private fun debugImportDatabase() {
        activityCallbackProvider.get()?.importHistoryDatabase()
    }
}

/** For Preview testing. */
val mockSettingsControllerImpl by lazy {
    object : SettingsController {
        override fun onBackPressed() {}

        override fun getTogglePreferenceSetter(settingsToggle: SettingsToggle): (Boolean) -> Unit {
            return { }
        }

        override fun getToggleState(settingsToggle: SettingsToggle): MutableState<Boolean> {
            return mutableStateOf(false)
        }

        override fun openUrl(uri: Uri, openViaIntent: Boolean) {}

        override fun getOnClickMap(): Map<Int, (() -> Unit)?> {
            return mapOf(
                R.string.settings_sign_in_to_join_neeva to { },
                R.string.settings_sign_out to { },
                R.string.settings_clear_browsing_data to { },
                R.string.settings_default_browser to { },
                R.string.archived_tabs_archive to { },
                R.string.settings_debug_local_feature_flags to { }
            )
        }

        override fun getOnDoubleClickMap(): Map<Int, (() -> Unit)?> = emptyMap()

        override fun getToggleChangedCallBackMap(): Map<String, (() -> Unit)?> = emptyMap()

        override fun isSignedOut(): Boolean { return false }

        override fun getNeevaUserInfoFlow(): StateFlow<UserInfo?> {
            return MutableStateFlow(
                UserInfo(
                    displayName = "Jehan Kobe Chang",
                    email = "kobec@neeva.co",
                    pictureURL = null
                )
            )
        }

        override fun signOut() {}

        override fun getContentFilterStrength(): ContentFilterModel.BlockingStrength {
            return ContentFilterModel.BlockingStrength.TRACKER_COOKIE
        }

        override fun setContentFilterStrength(
            strength: ContentFilterModel.BlockingStrength
        ) {}

        override fun getCookieNoticeSelection(): ContentFilterModel.CookieNoticeSelection {
            return ContentFilterModel.CookieNoticeSelection.DECLINE_COOKIES
        }

        override fun setCookieNoticeSelection(selection: ContentFilterModel.CookieNoticeSelection) {
        }

        override fun getCookieNoticePreferences(): Set<ContentFilterModel.CookieNoticeCookies> {
            return EnumSet.noneOf(ContentFilterModel.CookieNoticeCookies::class.java)
        }

        override fun setCookieNoticePreferences(
            preferences: Set<ContentFilterModel.CookieNoticeCookies>
        ) {}

        override fun clearBrowsingData(
            clearingOptions: Map<SettingsToggle, Boolean>,
            timeClearingOption: TimeClearingOption
        ) {}

        override fun getSetDefaultAndroidBrowserManager(): SetDefaultAndroidBrowserManager {
            return FakeSetDefaultAndroidBrowserManager()
        }

        override fun openAndroidDefaultBrowserSettings() { }

        override fun isAdvancedSettingsAllowed(): Boolean { return true }
    }
}
