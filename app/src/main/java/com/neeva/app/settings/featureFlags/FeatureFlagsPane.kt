package com.neeva.app.settings.featureFlags

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.getFakeSettingsViewModel
import com.neeva.app.settings.sharedComposables.SettingsPane
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun FeatureFlagsPane(
    settingsViewModel: SettingsViewModel
) {
    SettingsPane(settingsViewModel, FeatureFlagsPaneData)
}

@Preview(name = "Feature Flags Pane, 1x font size", locale = "en")
@Preview(name = "Feature Flags Pane, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Feature Flags Pane, RTL, 1x font size", locale = "he")
@Preview(name = "Feature Flags Pane, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingSettings_Preview() {
    NeevaTheme {
        FeatureFlagsPane(getFakeSettingsViewModel())
    }
}

@Preview(name = "Feature Flags Pane Dark, 1x font size", locale = "en")
@Preview(name = "Feature Flags Pane Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Feature Flags Pane Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Feature Flags Pane Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingSettings_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        FeatureFlagsPane(getFakeSettingsViewModel())
    }
}
