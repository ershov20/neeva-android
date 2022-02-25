package com.neeva.app.settings

import android.net.Uri

data class SettingsGroupData(
    val titleId: Int? = null,
    val rows: List<SettingsRowData>
)

data class SettingsRowData(
    val type: SettingsRowType,
    val titleId: Int,
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
    LABEL,
    LINK,
    TOGGLE,
    NAVIGATION,
    PROFILE,
    BUTTON,
    CLEAR_DATA_BUTTON
}

enum class SettingsToggle(
    val key: String,
    val defaultValue: Boolean
) {
    SHOW_SEARCH_SUGGESTIONS("SHOW_SEARCH_SUGGESTIONS", false),
    BLOCK_POP_UP_WINDOWS("BLOCK_POP_UP_WINDOWS", false),
    OFFER_TO_OPEN_COPIED_LINKS("OFFER_TO_OPEN_COPIED_LINKS", false),
    SHOW_LINK_PREVIEWS("SHOW_LINK_PREVIEWS", false),
    CLOSE_INCOGNITO_TABS("CLOSE_INCOGNITO_TABS", false),
    TRACKING_PROTECTION("TRACKING_PROTECTION", false),
    CLEAR_BROWSING_HISTORY("CLEAR_BROWSING_HISTORY", true),
    CLEAR_CACHE("CLEAR_CACHE", true),
    CLEAR_COOKIES("CLEAR_COOKIES", true),
    CLEAR_BROWSING_TRACKING_PROTECTION("CLEAR_BROWSING_TRACKING_PROTECTION", false),
    CLEAR_DOWNLOADED_FILES("CLEAR_DOWNLOADED_FILES", false),
}
