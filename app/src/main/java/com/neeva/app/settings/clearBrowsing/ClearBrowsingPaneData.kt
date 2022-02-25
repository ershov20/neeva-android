package com.neeva.app.settings.clearBrowsing

import android.net.Uri
import androidx.annotation.StringRes
import com.neeva.app.NeevaConstants.appManageMemory
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle

object ClearBrowsingPaneData {
    @StringRes
    val topAppBarTitleResId: Int = R.string.settings_clear_browsing_data
    val data = listOf(
        SettingsGroupData(
            R.string.settings_data_on_this_device,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_browsing_history,
                    togglePreferenceKey = SettingsToggle.CLEAR_BROWSING_HISTORY.key
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_cache,
                    togglePreferenceKey = SettingsToggle.CLEAR_CACHE.key
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_cookies_primary,
                    togglePreferenceKey = SettingsToggle.CLEAR_COOKIES.key
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_tracking_protection,
                    togglePreferenceKey = SettingsToggle.CLEAR_BROWSING_TRACKING_PROTECTION.key,
                    enabled = false
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_downloaded_files,
                    togglePreferenceKey = SettingsToggle.CLEAR_DOWNLOADED_FILES.key,
                    enabled = false
                ),
            )
        ),
        SettingsGroupData(
            rows = listOf(
                SettingsRowData(
                    type = SettingsRowType.CLEAR_DATA_BUTTON,
                    titleId = R.string.settings_clear_selected_data_on_device
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_data_in_neeva_memory,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_manage_neeva_memory,
                    url = Uri.parse(appManageMemory)
                )
            )
        ),
    )
}
