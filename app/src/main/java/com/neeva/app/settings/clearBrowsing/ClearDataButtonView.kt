package com.neeva.app.settings.clearBrowsing

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout

@Composable
fun ClearDataButtonView(
    getToggleState: (key: String?) -> MutableState<Boolean>?,
    rowData: SettingsRowData,
    onClearBrowsingData: (Map<String, Boolean>, TimeClearingOption) -> Unit
) {
    val title = stringResource(id = rowData.primaryLabelId)
    val showDialog = remember { mutableStateOf(false) }
    val cleared = remember { mutableStateOf(false) }

    ClearDataButton(
        text = title,
        cleared = cleared.value,
        openDialog = { showDialog.value = true }
    )
    if (showDialog.value) {
        ClearBrowsingDialog(
            confirmAction = { timeClearingOption ->
                onClearBrowsingData(
                    getClearingOptionsMap(
                        ClearBrowsingPaneData.data[0].rows,
                        getToggleState
                    ),
                    timeClearingOption
                )
                cleared.value = true
                showDialog.value = false
            },
            dismissAction = { showDialog.value = false }
        )
    }
}

@Composable
fun ClearDataButton(
    text: String,
    cleared: Boolean,
    openDialog: () -> Unit
) {
    val clearText = stringResource(id = R.string.settings_selected_data_cleared_success)
    val onClick = openDialog.takeIf { !cleared }
    BaseRowLayout(onTapRow = onClick) {
        if (cleared) {
            Text(
                text = clearText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun getClearingOptionsMap(
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

class ClearDataButtonPreviews {
    @Preview("ClearDataButton 1x", locale = "en")
    @Preview("ClearDataButton 2x", locale = "en", fontScale = 2.0f)
    @Preview("ClearDataButton RTL, 1x", locale = "he")
    @Composable
    fun Default() {
        OneBooleanPreviewContainer { isCleared ->
            ClearDataButton(
                text = stringResource(R.string.debug_long_string_primary),
                cleared = isCleared,
                openDialog = {}
            )
        }
    }
}
