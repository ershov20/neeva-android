package com.neeva.app.settings.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
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
fun MainSettingsPane(
    settingsViewModel: SettingsViewModel
) {
    val buttonClickMap = settingsViewModel.getMainSettingsNavigation()
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        FullScreenDialogTopBar(
            title = stringResource(MainSettingsData.topAppBarTitleResId),
            onBackPressed = settingsViewModel::onBackPressed
        )

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
        ) {
            MainSettingsData.data.forEach { groupData ->
                item {
                    SettingsGroupView(
                        settingsViewModel,
                        groupData,
                        buttonOnClicks = buttonClickMap
                    )
                }
            }
        }
    }
}

@Preview(name = "Main settings, 1x font size", locale = "en")
@Preview(name = "Main settings, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Main settings, RTL, 1x font size", locale = "he")
@Preview(name = "Main settings, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsMain_Preview() {
    NeevaTheme {
        MainSettingsPane(
            getFakeSettingsViewModel()
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
            getFakeSettingsViewModel()
        )
    }
}
