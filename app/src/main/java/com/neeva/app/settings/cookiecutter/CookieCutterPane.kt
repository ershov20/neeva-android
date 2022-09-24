// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.cookiecutter

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.NeevaConstants
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CookieCutterPane(settingsController: SettingsController, neevaConstants: NeevaConstants) {
    val isStrictModeEnabled: @Composable () -> Boolean = {
        settingsController.getCookieCutterStrength() ==
            CookieCutterModel.BlockingStrength.TRACKER_REQUEST
    }

    val isEnabled: Boolean =
        settingsController.getToggleState(SettingsToggle.TRACKING_PROTECTION).value

    SettingsPane(
        settingsController,
        CookieCutterPaneData(neevaConstants, isEnabled, isStrictModeEnabled)
    )
}

@Preview(name = "Cookie Cutter Pane, 1x font size", locale = "en")
@Preview(name = "Cookie Cutter Pane, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Cookie Cutter Pane, RTL, 1x font size", locale = "he")
@Preview(name = "Cookie Cutter Pane, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun CookieCutterSettings_Preview() {
    NeevaTheme {
        CookieCutterPane(mockSettingsControllerImpl, NeevaConstants())
    }
}

@Preview(name = "Cookie Cutter Pane Dark, 1x font size", locale = "en")
@Preview(name = "Cookie Cutter Pane Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Cookie Cutter Pane Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Cookie Cutter Pane Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun CookieCutterSettings_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        CookieCutterPane(mockSettingsControllerImpl, NeevaConstants())
    }
}
