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
    val togglePreferenceKey: String? = null,
    val enabled: Boolean = true
)

enum class SettingsRowType {
    LABEL,
    LINK,
    TOGGLE,
    NAVIGATION,
    PROFILE,
    BUTTON
}

enum class SettingsToggle(
    val key: String,
    val defaultValue: Boolean
) {
    SHOW_SEARCH_SUGGESTIONS("SHOW_SEARCH_SUGGESTIONS", true),
    BLOCK_POP_UP_WINDOWS("BLOCK_POP_UP_WINDOWS", true),
    OFFER_TO_OPEN_COPIED_LINKS("OFFER_TO_OPEN_COPIED_LINKS", false),
    SHOW_LINK_PREVIEWS("SHOW_LINK_PREVIEWS", false),
    CLOSE_INCOGNITO_TABS("CLOSE_INCOGNITO_TABS", false),
    TRACKING_PROTECTION("TRACKING_PROTECTION", true),
    CLEAR_BROWSING_HISTORY("CLEAR_BROWSING_HISTORY", true),
    CLEAR_CACHE("CLEAR_CACHE", true),
    CLEAR_COOKIES("CLEAR_COOKIES", true),
    CLEAR_BROWSING_TRACKING_PROTECTION("CLEAR_BROWSING_TRACKING_PROTECTION", true),
    CLEAR_DOWNLOADED_FILES("CLEAR_DOWNLOADED_FILES", false),
}
