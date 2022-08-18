package com.neeva.app.settings.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.neeva.app.LocalChromiumVersion
import com.neeva.app.NeevaConstants
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews

@Composable
fun MainSettingsPane(settingsController: SettingsController, neevaConstants: NeevaConstants) {
    SettingsPane(settingsController, MainSettingsData(neevaConstants))
}

@PortraitPreviews
@Composable
fun SettingsMain_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false, addBorder = false) {
        CompositionLocalProvider(LocalChromiumVersion provides "XXX.XXX.XXX.XXX") {
            MainSettingsPane(mockSettingsControllerImpl, NeevaConstants())
        }
    }
}

@PortraitPreviews
@Composable
fun SettingsMain_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true, addBorder = false) {
        CompositionLocalProvider(LocalChromiumVersion provides "XXX.XXX.XXX.XXX") {
            MainSettingsPane(mockSettingsControllerImpl, NeevaConstants())
        }
    }
}
