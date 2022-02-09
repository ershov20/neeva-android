package com.neeva.app.settings.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.SettingsRow
import com.neeva.app.settings.SettingsTopAppBar
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.getFakeSettingsViewModel
import com.neeva.app.ui.theme.NeevaTheme
import java.util.Locale

@Composable
fun MainSettingsPane(
    settingsViewModel: SettingsViewModel,
    isSignedOut: () -> Boolean
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .fillMaxSize(),
    ) {
        SettingsTopAppBar(
            title = stringResource(MainSettingsData.topAppBarTitleResId),
            onBackPressed = settingsViewModel::onBackPressed
        )

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            MainSettingsData.data.forEach {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(16.dp)
                            .wrapContentHeight(align = Alignment.Bottom),
                    ) {
                        if (it.titleId != null) {
                            Text(
                                text = stringResource(it.titleId).uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                val onClickMap = mutableMapOf(
                    R.string.settings_clear_browsing_data to
                        settingsViewModel::showClearBrowsingSettings,
                    R.string.settings_sign_in_to_join_neeva to
                        settingsViewModel::showProfileSettings,
                )
                if (isSignedOut()) {
                    onClickMap[R.string.settings_sign_in_to_join_neeva] =
                        settingsViewModel::showFirstRun
                }

                items(it.rows) { rowData ->
                    val rowModifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                    SettingsRow(
                        rowData = rowData,
                        settingsViewModel = settingsViewModel,
                        onClick = onClickMap[rowData.titleId] ?: {},
                        modifier = rowModifier
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
            getFakeSettingsViewModel(),
            isSignedOut = { true }
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
            getFakeSettingsViewModel(),
            isSignedOut = { false }
        )
    }
}
