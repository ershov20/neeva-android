package com.neeva.app.settings.cookieCutter

import androidx.annotation.StringRes
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle

class CookieCutterPaneData(neevaConstants: NeevaConstants) : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings_cookie_cutter
    override val shouldShowUserName: Boolean = false
    override val data = listOf(
        SettingsGroupData(
            rows = listOf(
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.TRACKING_PROTECTION
                )
            )
        ),
        SettingsGroupData(
            R.string.settings_trackers,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.RADIO_BUTTON,
                )
            )
        ),
    )
}
