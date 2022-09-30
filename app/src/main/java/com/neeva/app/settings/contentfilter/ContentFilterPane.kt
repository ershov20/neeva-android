// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.contentfilter

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.NeevaConstants
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.ui.theme.NeevaTheme

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

@Preview(name = "Content Filter Pane, 1x font size", locale = "en")
@Preview(name = "Content Filter Pane, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Content Filter Pane, RTL, 1x font size", locale = "he")
@Preview(name = "Content Filter Pane, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ContentFilterSettings_Preview() {
    NeevaTheme {
        ContentFilterPane(mockSettingsControllerImpl, NeevaConstants())
    }
}

@Preview(name = "Content Filter Pane Dark, 1x font size", locale = "en")
@Preview(name = "Content Filter Pane Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Content Filter Pane Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Content Filter Pane Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ContentFilterSettings_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        ContentFilterPane(mockSettingsControllerImpl, NeevaConstants())
    }
}
