package com.neeva.app.settings.clearBrowsing

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedComposables.SettingsPane
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ClearBrowsingPane(
    settingsController: SettingsController
) {
    SettingsPane(settingsController, ClearBrowsingPaneData)
}

@Preview(name = "Clear Browsing Pane, 1x font size", locale = "en")
@Preview(name = "Clear Browsing Pane, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Clear Browsing Pane, RTL, 1x font size", locale = "he")
@Preview(name = "Clear Browsing Pane, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingSettings_Preview() {
    NeevaTheme {
        ClearBrowsingPane(mockSettingsControllerImpl)
    }
}

@Preview(name = "Clear Browsing Pane Dark, 1x font size", locale = "en")
@Preview(name = "Clear Browsing Pane Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Clear Browsing Pane Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Clear Browsing Pane Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingSettings_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        ClearBrowsingPane(mockSettingsControllerImpl)
    }
}
