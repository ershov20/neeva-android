package com.neeva.app.settings

import android.net.Uri
import androidx.annotation.StringRes
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
    val primaryLabelId: Int,
    val secondaryLabelId: Int? = null,
    val url: Uri? = null,

    /** If the setting is stored in SharedPreferences: this indicates its key. */
    val togglePreferenceKey: String? = null,

    /** Whether or not to open the URL by firing an Intent, allow other apps to capture it. */
    val openUrlViaIntent: Boolean = false,

    /** Whether or not the user can interact with the menu item. */
    val enabled: Boolean = true,

    /** Intended for use with ProfileRow. */
    val showSSOProviderAsPrimaryLabel: Boolean = false
)

enum class SettingsRowType {
    LINK,
    TOGGLE,
    NAVIGATION,
    PROFILE,
    BUTTON,
    CLEAR_DATA_BUTTON,
    SUBSCRIPTION
}

enum class SettingsToggle(
    val key: String,
    val defaultValue: Boolean
) {
    SHOW_SEARCH_SUGGESTIONS("SHOW_SEARCH_SUGGESTIONS", true),
    BLOCK_POP_UP_WINDOWS("BLOCK_POP_UP_WINDOWS", false),
    REQUIRE_CONFIRMATION_ON_TAB_CLOSE("REQUIRE_CONFIRMATION_ON_TAB_CLOSE", false),
    CLOSE_INCOGNITO_TABS("CLOSE_INCOGNITO_TABS", false),
    TRACKING_PROTECTION("TRACKING_PROTECTION", false),
    CLEAR_BROWSING_HISTORY("CLEAR_BROWSING_HISTORY", true),
    CLEAR_CACHE("CLEAR_CACHE", true),
    CLEAR_COOKIES("CLEAR_COOKIES", true),
    CLEAR_BROWSING_TRACKING_PROTECTION("CLEAR_BROWSING_TRACKING_PROTECTION", false),
    CLEAR_DOWNLOADED_FILES("CLEAR_DOWNLOADED_FILES", false),
    IS_ADVANCED_SETTINGS_ALLOWED("IS_ADVANCED_SETTINGS_ALLOWED", false)
}

enum class LocalDebugFlags(
    @StringRes val flagDisplayName_stringId: Int,
    val defaultValue: Boolean
) {
    DEBUG_ENABLE_INCOGNITO_SCREENSHOTS(R.string.settings_debug_enable_incognito_screenshots, false),
    DEBUG_ENABLE_SHOW_DESKTOP_SITE(R.string.settings_debug_enable_show_desktop_site, false);

    val key: String = name
}
