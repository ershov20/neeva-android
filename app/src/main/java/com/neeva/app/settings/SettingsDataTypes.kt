package com.neeva.app.settings

import android.net.Uri
import com.neeva.app.R

interface SettingsPaneDataInterface {
    val topAppBarTitleResId: Int
    val shouldShowUserName: Boolean
    val data: List<SettingsGroupData>
}

data class SettingsGroupData(
    val titleId: Int? = null,
    val rows: List<SettingsRowData>,
    val isForDebugOnly: Boolean = false
)

data class SettingsRowData(
    val type: SettingsRowType,
    val primaryLabelId: Int? = null,
    val secondaryLabelId: Int? = null,
    val url: Uri? = null,

    /** If the setting is a [SettingsToggle] this is its value */
    val settingsToggle: SettingsToggle? = null,

    /** Whether or not to open the URL by firing an Intent, allow other apps to capture it. */
    val openUrlViaIntent: Boolean = false,

    /** Whether or not the user can interact with the menu item. */
    val enabled: Boolean = true,

    /** Intended for use with ProfileRow. */
    val showSSOProviderAsPrimaryLabel: Boolean = false
)

enum class SettingsRowType {
    LINK,
    TEXT,
    TOGGLE,
    NAVIGATION,
    PROFILE,
    BUTTON,
    CLEAR_DATA_BUTTON,
    SUBSCRIPTION,
    COOKIE_CUTTER_BLOCKING_STRENGTH,
    COOKIE_CUTTER_NOTICE_SELECTION,
    COOKIE_PREFERENCE_SELECTION,
}

enum class SettingsToggle(
    val primaryLabelId: Int? = null,
    val secondaryLabelId: Int? = null,
    val defaultValue: Boolean,
    val isAdvancedSetting: Boolean = false
) {
    SHOW_SEARCH_SUGGESTIONS(
        primaryLabelId = R.string.settings_show_search_search_suggestions,
        defaultValue = true
    ),
    REQUIRE_CONFIRMATION_ON_TAB_CLOSE(
        primaryLabelId = R.string.settings_confirm_close_all_tabs_title,
        secondaryLabelId = R.string.settings_confirm_close_all_tabs_body,
        defaultValue = false
    ),
    CLOSE_INCOGNITO_TABS(
        primaryLabelId = R.string.settings_close_incognito_when_switching_title,
        secondaryLabelId = R.string.settings_close_incognito_when_switching_body,
        defaultValue = false
    ),
    TRACKING_PROTECTION(
        primaryLabelId = R.string.cookie_cutter,
        defaultValue = true
    ),
    LOGGING_CONSENT(
        primaryLabelId = R.string.logging_consent_toggle_title,
        secondaryLabelId = R.string.logging_consent_toggle_subtitle,
        defaultValue = true
    ),
    CLEAR_BROWSING_HISTORY(
        primaryLabelId = R.string.settings_browsing_history,
        defaultValue = true
    ),
    CLEAR_CACHE(
        primaryLabelId = R.string.settings_cache,
        defaultValue = true
    ),
    CLEAR_COOKIES(
        primaryLabelId = R.string.settings_cookies_primary,
        secondaryLabelId = R.string.settings_cookies_secondary,
        defaultValue = true
    ),
    CLEAR_DOWNLOADED_FILES(
        primaryLabelId = R.string.settings_clear_downloaded_files,
        defaultValue = false
    ),
    CLEAR_BROWSING_TRACKING_PROTECTION(
        primaryLabelId = R.string.tracking_protection,
        defaultValue = false
    ),
    IS_ADVANCED_SETTINGS_ALLOWED(
        defaultValue = false
    ),

    // Advanced / development settings:
    AUTOMATED_TAB_MANAGEMENT(
        primaryLabelId = R.string.settings_debug_automated_tab_management,
        defaultValue = false,
        isAdvancedSetting = true
    ),
    DEBUG_ENABLE_INCOGNITO_SCREENSHOTS(
        primaryLabelId = R.string.settings_debug_enable_incognito_screenshots,
        defaultValue = false,
        isAdvancedSetting = true
    ),
    DEBUG_ENABLE_SHOW_DESKTOP_SITE(
        primaryLabelId = R.string.settings_debug_enable_show_desktop_site,
        defaultValue = false,
        isAdvancedSetting = true
    ),
    DEBUG_M1_APP_HOST(
        primaryLabelId = R.string.settings_debug_m1_apphost,
        defaultValue = false,
        isAdvancedSetting = true
    ),
    DEBUG_LOCAL_NEEVA_DEV_APP_HOST(
        primaryLabelId = R.string.settings_debug_local_neeva_dev_apphost,
        defaultValue = false,
        isAdvancedSetting = true
    ),
    DEBUG_NEEVASCOPE(
        primaryLabelId = R.string.settings_debug_neevascope,
        defaultValue = false,
        isAdvancedSetting = true
    );

    val key: String = name
}
