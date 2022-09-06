// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
    val showSSOProviderAsPrimaryLabel: Boolean = false,

    /** If set, will provide a secondary label that overrides [secondaryLabelId]. */
    val secondaryLabelLambda: @Composable (() -> String)? = null
) {
    @Composable
    fun getSecondaryLabel(): String? {
        return secondaryLabelLambda?.invoke()
            ?: secondaryLabelId?.let { stringResource(it) }
    }
}

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
    val defaultValue: Boolean
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
        primaryLabelId = R.string.settings_automated_tab_management,
        defaultValue = false
    ),
    DEBUG_ENABLE_DISPLAY_TABS_BY_REVERSE_CREATION_TIME(
        primaryLabelId = R.string.settings_debug_tabs_in_reverse,
        defaultValue = false,
    ),
    DEBUG_ENABLE_INCOGNITO_SCREENSHOTS(
        primaryLabelId = R.string.settings_debug_enable_incognito_screenshots,
        defaultValue = false
    ),
    DEBUG_USE_CUSTOM_DOMAIN(
        primaryLabelId = R.string.settings_debug_use_custom_neeva_domain,
        defaultValue = false
    ),
    ENABLE_NEEVASCOPE(
        primaryLabelId = R.string.settings_neevascope,
        defaultValue = false
    );

    val key: String = name
}
