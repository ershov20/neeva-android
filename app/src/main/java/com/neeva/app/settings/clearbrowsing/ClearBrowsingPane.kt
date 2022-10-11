// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.clearbrowsing

import androidx.compose.runtime.Composable
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.NeevaConstants
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark

@Composable
fun ClearBrowsingPane(settingsController: SettingsController, neevaConstants: NeevaConstants) {
    SettingsPane(settingsController, ClearBrowsingPaneData(neevaConstants))
}

@PortraitPreviews
@Composable
fun ClearBrowsingSettings_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = false,
        addBorder = false
    ) {
        ClearBrowsingPane(mockSettingsControllerImpl, neevaConstants = LocalNeevaConstants.current)
    }
}

@PortraitPreviewsDark
@Composable
fun ClearBrowsingSettings_Dark_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = true,
        addBorder = false
    ) {
        ClearBrowsingPane(mockSettingsControllerImpl, neevaConstants = LocalNeevaConstants.current)
    }
}
