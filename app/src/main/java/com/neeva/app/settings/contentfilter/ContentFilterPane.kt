// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.contentfilter

import androidx.compose.runtime.Composable
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.NeevaConstants
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark

@Composable
fun ContentFilterPane(settingsController: SettingsController, neevaConstants: NeevaConstants) {
    val isStrictModeEnabled: @Composable () -> Boolean = {
        settingsController.getContentFilterStrength() ==
            ContentFilterModel.BlockingStrength.TRACKER_REQUEST
    }

    val isEnabled: Boolean =
        settingsController.getToggleState(SettingsToggle.TRACKING_PROTECTION).value

    SettingsPane(
        settingsController,
        ContentFilterPaneData(neevaConstants, isEnabled, isStrictModeEnabled)
    )
}

@PortraitPreviews
@Composable
fun ContentFilterSettings_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = false,
        addBorder = false
    ) {
        ContentFilterPane(mockSettingsControllerImpl, LocalNeevaConstants.current)
    }
}

@PortraitPreviewsDark
@Composable
fun ContentFilterSettings_Dark_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = true,
        addBorder = false
    ) {
        ContentFilterPane(mockSettingsControllerImpl, LocalNeevaConstants.current)
    }
}
