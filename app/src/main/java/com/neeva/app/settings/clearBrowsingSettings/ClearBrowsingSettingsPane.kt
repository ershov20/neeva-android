package com.neeva.app.settings.clearBrowsingSettings

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
import com.neeva.app.settings.SettingsPaneListener
import com.neeva.app.settings.SettingsRow
import com.neeva.app.settings.SettingsTopAppBar
import com.neeva.app.settings.mainSettings.getFakeSettingsPaneListener
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ClearBrowsingSettingsPane(settingsPaneListener: SettingsPaneListener) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        SettingsTopAppBar(
            title = stringResource(ClearBrowsingSettingsData.topAppBarTitleResId),
            onBackPressed = settingsPaneListener.onBackPressed
        )

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            ClearBrowsingSettingsData.data.forEach {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .wrapContentHeight(align = Alignment.Bottom),
                    ) {
                        // TODO(kobec): might be wrong font style
                        if (it.titleId != null) {
                            Text(
                                text = stringResource(it.titleId),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }

                items(it.rows) { rowData ->
                    val rowModifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.surface)

                    if (rowData.titleId == R.string.settings_clear_selected_data_on_device) {
                        val title = stringResource(id = rowData.titleId)
                        ClearBrowsingDataButton(
                            text = title,
                            clearHistory = settingsPaneListener.onClearHistory,
                            modifier = rowModifier
                        )
                    } else {
                        SettingsRow(
                            rowData = rowData,
                            openUrl = settingsPaneListener.openUrl,
                            getTogglePreferenceSetter = settingsPaneListener
                                .getTogglePreferenceSetter,
                            getToggleState = settingsPaneListener.getToggleState,
                            modifier = rowModifier
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
        ClearBrowsingSettingsPane(getFakeSettingsPaneListener())
    }
}

@Preview(name = "Clear Browsing Pane Dark, 1x font size", locale = "en")
@Preview(name = "Clear Browsing Pane Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Clear Browsing Pane Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Clear Browsing Pane Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingSettings_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        ClearBrowsingSettingsPane(getFakeSettingsPaneListener())
    }
}
