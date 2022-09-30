// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.contentfilter

import androidx.compose.runtime.Composable
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CookiePreferencesPane(settingsController: SettingsController) {
    SettingsPane(settingsController, CookiePreferencesPaneData())
}

@PortraitPreviews
@Composable
fun CookiePreferences_Preview() {
    NeevaTheme {
        CookiePreferencesPane(mockSettingsControllerImpl)
    }
}

@PortraitPreviews
@Composable
fun CookiePreferences_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        CookiePreferencesPane(mockSettingsControllerImpl)
    }
}
