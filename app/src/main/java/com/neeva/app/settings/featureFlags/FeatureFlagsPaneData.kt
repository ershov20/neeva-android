package com.neeva.app.settings.featureFlags

import androidx.annotation.StringRes
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle

object FeatureFlagsPaneData : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings_debug_local_feature_flags
    override val shouldShowUserName: Boolean = false
    private val allDebugFlags = SettingsToggle.values()
        .filter { it.isAdvancedSetting }
        .map {
            SettingsRowData(
                type = SettingsRowType.TOGGLE,
                settingsToggle = it
            )
        }
    override val data = listOf(
        SettingsGroupData(
            R.string.settings_debug_flags,
            allDebugFlags
        ),
        SettingsGroupData(
            R.string.settings_debug_actions,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_debug_open_50_tabs
                ),
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_debug_export_database
                )
            )
        )
    )
}
