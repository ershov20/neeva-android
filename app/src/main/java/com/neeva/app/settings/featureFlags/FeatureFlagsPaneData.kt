package com.neeva.app.settings.featureFlags

import androidx.annotation.StringRes
import com.neeva.app.R
import com.neeva.app.settings.LocalDebugFlags
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType

object FeatureFlagsPaneData : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings_debug_local_feature_flags
    override val shouldShowUserName: Boolean = false
    private val allFeatureFlags = LocalDebugFlags.values().map {
        SettingsRowData(
            type = SettingsRowType.TOGGLE,
            titleId = it.flagDisplayName_stringId,
            togglePreferenceKey = LocalDebugFlags.DEBUG_BOTTOM_URL_BAR.key
        )
    }
    override val data = listOf(
        SettingsGroupData(
            R.string.settings_debug_flags,
            allFeatureFlags
        )
    )
}
