package com.neeva.app.settings.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.getFakeSettingsViewModel
import com.neeva.app.settings.sharedComposables.SettingsPane
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ProfileSettingsPane(
    settingsViewModel: SettingsViewModel
) {
    SettingsPane(settingsViewModel, ProfileSettingsPaneData)
}

@Preview(name = "Settings Profile, 1x font size", locale = "en")
@Preview(name = "Settings Profile, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Profile, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Profile, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsProfile_Preview() {
    NeevaTheme {
        ProfileSettingsPane(
            settingsViewModel = getFakeSettingsViewModel()
        )
    }
}

@Preview(name = "Settings Profile Dark, 1x font size", locale = "en")
@Preview(name = "Settings Profile Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Profile Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Profile Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsProfile_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        ProfileSettingsPane(
            settingsViewModel = getFakeSettingsViewModel()
        )
    }
}
