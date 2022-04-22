package com.neeva.app.settings.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedComposables.SettingsPane
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun MainSettingsPane(
    settingsController: SettingsController
) {
    SettingsPane(settingsController, MainSettingsData)
}

@Preview(name = "Main settings, 1x font size", locale = "en")
@Preview(name = "Main settings, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Main settings, RTL, 1x font size", locale = "he")
@Preview(name = "Main settings, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsMain_Preview() {
    NeevaTheme {
        MainSettingsPane(
            mockSettingsControllerImpl
        )
    }
}

@Preview(name = "Main settings Dark, 1x font size", locale = "en")
@Preview(name = "Main settings Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Main settings Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Main settings Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsMain_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        MainSettingsPane(
            mockSettingsControllerImpl
        )
    }
}
