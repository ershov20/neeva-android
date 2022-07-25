package com.neeva.app.settings.cookiecutter

import androidx.compose.runtime.Composable
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedComposables.SettingsPane
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
