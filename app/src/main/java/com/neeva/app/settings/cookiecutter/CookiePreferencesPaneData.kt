// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.cookiecutter

import androidx.annotation.StringRes
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType

class CookiePreferencesPaneData : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings_cookie_preferences
    override val shouldShowUserName: Boolean = false
    override val data = listOf(
        SettingsGroupData(
            rows = listOf(
                SettingsRowData(
                    type = SettingsRowType.COOKIE_PREFERENCE_SELECTION
                ),
                SettingsRowData(
                    type = SettingsRowType.TEXT,
                    primaryLabelId = R.string.cookie_cutting_individual_disclaimer
                )
            )
        )
    )
}
