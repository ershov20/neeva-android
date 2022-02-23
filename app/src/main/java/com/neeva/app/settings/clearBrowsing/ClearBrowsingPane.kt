package com.neeva.app.settings.clearBrowsing

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.SettingsRow
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.getFakeSettingsViewModel
import com.neeva.app.ui.theme.FullScreenDialogTopBar
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ClearBrowsingPane(
    settingsViewModel: SettingsViewModel,
    onClearBrowsingData: (MutableMap<String, Boolean>) -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .fillMaxSize(),
    ) {
        FullScreenDialogTopBar(
            title = stringResource(ClearBrowsingData.topAppBarTitleResId),
            onBackPressed = settingsViewModel::onBackPressed
        )

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            ClearBrowsingData.data.forEach {
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
                                text = stringResource(it.titleId),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
                items(it.rows) { rowData ->
                    val rowModifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)

                    if (rowData.titleId == R.string.settings_clear_selected_data_on_device) {
                        val title = stringResource(id = rowData.titleId)
                        val showDialog = remember { mutableStateOf(false) }
                        val cleared = remember { mutableStateOf(false) }
                        ClearDataButton(
                            text = title,
                            cleared = cleared,
                            onClick = {
                                showDialog.value = true
                            },
                            modifier = rowModifier
                        )
                        if (showDialog.value) {
                            SettingsAlertDialogue(
                                text = stringResource(id = R.string.clear_browsing_dialog_text),
                                confirmString = stringResource(
                                    id = R.string.clear_browsing_clear_data
                                ),
                                confirmAction = {
                                    onClearBrowsingData(
                                        getClearingOptions(
                                            ClearBrowsingData.data[0].rows,
                                            settingsViewModel::getToggleState
                                        )
                                    )
                                    cleared.value = true
                                    showDialog.value = false
                                },
                                dismissString = stringResource(id = R.string.cancel),
                                dismissAction = { showDialog.value = false }
                            )
                        }
                    } else {
                        SettingsRow(
                            rowData = rowData,
                            settingsViewModel = settingsViewModel,
                            modifier = rowModifier
                        )
                    }
                }
            }
        }
    }
}

fun getClearingOptions(
    rowData: List<SettingsRowData>,
    getToggleState: (String?) -> MutableState<Boolean>?
): MutableMap<String, Boolean> {
    val clearingOptions = mutableMapOf<String, Boolean>()
    rowData.forEach {
        if (it.togglePreferenceKey != null) {
            val clearOptionValue = getToggleState(it.togglePreferenceKey)?.value ?: false
            clearingOptions[it.togglePreferenceKey] = clearOptionValue
        }
    }
    return clearingOptions
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
