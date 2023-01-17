// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.featureflags

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPrefFolder

object FeatureFlagsPaneData : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings_debug_local_feature_flags
    override val shouldShowUserName: Boolean = false

    override val data = listOf(
        SettingsGroupData(
            R.string.settings_debug_custom_neeva_domain,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.DEBUG_USE_CUSTOM_DOMAIN
                ),
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_debug_custom_neeva_domain_current,
                    secondaryLabelLambda = @Composable {
                        SharedPrefFolder.App.CustomNeevaDomain
                            .getFlow(LocalSharedPreferencesModel.current)
                            .collectAsState()
                            .value
                    }
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_debug_flags,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.DEBUG_ENABLE_INCOGNITO_SCREENSHOTS
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle =
                    SettingsToggle.DEBUG_ENABLE_DISPLAY_TABS_BY_REVERSE_CREATION_TIME
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.DEBUG_ENABLE_INVITE_USER_TO_SPACE
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.DEBUG_USE_CUSTOM_TABS_FOR_LOGIN
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.DEBUG_ENABLE_NATIVE_GOOGLE_LOGIN
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.DEBUG_ENABLE_BILLING
                )
            )
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
                    primaryLabelId = R.string.settings_debug_open_500_tabs
                ),
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_debug_simulate_first_run
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_debug_database,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_debug_export_database
                ),
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_debug_import_database,
                    secondaryLabelId = R.string.settings_debug_import_database_description
                )
            )
        )
    )
}
