package com.neeva.app.settings.cookiecutter

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.NeevaConstants
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedComposables.SettingsPane
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CookieCutterPane(settingsController: SettingsController, neevaConstants: NeevaConstants) {
    SettingsPane(
        settingsController,
        CookieCutterPaneData(
            neevaConstants,
            settingsController.getToggleState(SettingsToggle.DEBUG_COOKIE_NOTICES)
        )
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
