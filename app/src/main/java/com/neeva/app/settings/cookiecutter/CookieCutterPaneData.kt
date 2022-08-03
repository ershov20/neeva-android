package com.neeva.app.settings.cookiecutter

import android.net.Uri
import androidx.annotation.StringRes
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle

class CookieCutterPaneData(
    neevaConstants: NeevaConstants
) : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.cookie_cutter
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
                    type = SettingsRowType.COOKIE_CUTTER_BLOCKING_STRENGTH
                )
            )
        ),
        SettingsGroupData(
            R.string.settings_cookie_notices,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.COOKIE_CUTTER_NOTICE_SELECTION
                ),
                SettingsRowData(
                    type = SettingsRowType.TEXT,
                    primaryLabelId = R.string.cookie_cutting_essential_disclaimer
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.learn_more,
                    url = Uri.parse(neevaConstants.cookieCutterLearnMoreUrl)
                )
            )
        )

    )
}
