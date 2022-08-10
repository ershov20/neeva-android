package com.neeva.app.settings.clearbrowsing

import android.net.Uri
import androidx.annotation.StringRes
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle

class ClearBrowsingPaneData(neevaConstants: NeevaConstants) : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings_clear_browsing_data
    override val shouldShowUserName: Boolean = false
    override val data = listOf(
        SettingsGroupData(
            R.string.clear_browsing_data_on_this_device,
            timeClearingOptionToggles
        ),
        SettingsGroupData(
            rows = listOf(
                SettingsRowData(
                    type = SettingsRowType.CLEAR_DATA_BUTTON,
                    primaryLabelId = R.string.settings_clear_selected_data_on_device
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_data_in_neeva_memory,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_manage_neeva_memory,
                    url = Uri.parse(neevaConstants.appManageMemory)
                )
            )
        ),
    )
    companion object {
        val timeClearingOptionToggles = listOf(
            SettingsRowData(
                type = SettingsRowType.TOGGLE,
                settingsToggle = SettingsToggle.CLEAR_BROWSING_HISTORY
            ),
            SettingsRowData(
                type = SettingsRowType.TOGGLE,
                settingsToggle = SettingsToggle.CLEAR_CACHE
            ),
            SettingsRowData(
                type = SettingsRowType.TOGGLE,
                settingsToggle = SettingsToggle.CLEAR_COOKIES
            ),
            SettingsRowData(
                type = SettingsRowType.TOGGLE,
                settingsToggle = SettingsToggle.CLEAR_BROWSING_TRACKING_PROTECTION,
                enabled = false
            )
        )
    }
}

enum class TimeClearingOption(@StringRes val string_id: Int) {
    LAST_HOUR(R.string.clear_browsing_last_hour),
    TODAY(R.string.clear_browsing_today),
    TODAY_AND_YESTERDAY(R.string.clear_browsing_today_and_yesterday),
    EVERYTHING(R.string.clear_browsing_everything)
}

object TimeClearingOptionsConstants {
    val sharedPrefKey = "CLEAR_BROWSING_TIME_CLEARING_OPTION"
}
