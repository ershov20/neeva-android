package com.neeva.app.settings.clearBrowsing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.getFakeSettingsViewModel
import com.neeva.app.settings.sharedComposables.SettingsGroupView
import com.neeva.app.ui.theme.FullScreenDialogTopBar
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ClearBrowsingPane(
    settingsViewModel: SettingsViewModel,
    onClearBrowsingData: (Map<String, Boolean>) -> Unit
) {
    Surface {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .fillMaxSize(),
        ) {
            FullScreenDialogTopBar(
                title = stringResource(ClearBrowsingPaneData.topAppBarTitleResId),
                onBackPressed = settingsViewModel::onBackPressed
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1.0f)
            ) {
                ClearBrowsingPaneData.data.forEach { groupData ->
                    item {
                        SettingsGroupView(
                            settingsViewModel,
                            groupData,
                            onClearBrowsingData
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Clear Browsing Pane, 1x font size", locale = "en")
@Preview(name = "Clear Browsing Pane, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Clear Browsing Pane, RTL, 1x font size", locale = "he")
@Preview(name = "Clear Browsing Pane, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingSettings_Preview() {
    NeevaTheme {
        ClearBrowsingPane(getFakeSettingsViewModel(), {})
    }
}

@Preview(name = "Clear Browsing Pane Dark, 1x font size", locale = "en")
@Preview(name = "Clear Browsing Pane Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Clear Browsing Pane Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Clear Browsing Pane Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingSettings_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        ClearBrowsingPane(getFakeSettingsViewModel(), {})
    }
}
