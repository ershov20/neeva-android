package com.neeva.app.settings.clearBrowsing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ClearDataButtonView(
    getToggleState: (key: String?) -> MutableState<Boolean>?,
    rowData: SettingsRowData,
    onClearBrowsingData: (Map<String, Boolean>) -> Unit,
    rowModifier: Modifier
) {
    val title = stringResource(id = rowData.titleId)
    val showDialog = remember { mutableStateOf(false) }
    val cleared = remember { mutableStateOf(false) }
    ClearDataButton(
        text = title,
        cleared = cleared,
        onClick = {
            showDialog.value = true
        },
        rowModifier = rowModifier
    )
    if (showDialog.value) {
        SettingsAlertDialogue(
            text = stringResource(id = R.string.clear_browsing_dialog_text),
            confirmString = stringResource(id = R.string.clear_browsing_clear_data),
            confirmAction = {
                onClearBrowsingData(
                    getClearingOptionsMap(
                        ClearBrowsingPaneData.data[0].rows,
                        getToggleState
                    )
                )
                cleared.value = true
                showDialog.value = false
            },
            dismissString = stringResource(id = R.string.cancel),
            dismissAction = { showDialog.value = false }
        )
    }
}

@Composable
fun ClearDataButton(
    text: String,
    cleared: MutableState<Boolean>,
    onClick: () -> Unit,
    rowModifier: Modifier
) {
    val clearText = stringResource(id = R.string.settings_selected_data_cleared_success)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(
                if (cleared.value) {
                    Modifier
                } else {
                    Modifier.clickable {
                        onClick()
                    }
                }
            ).then(rowModifier)
    ) {
        if (cleared.value) {
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

class ClearDataButtonPreviews :
    BooleanPreviewParameterProvider<ClearDataButtonPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val isCleared: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isCleared = booleanArray[1]
    )

    @Preview("ClearDataButton 1x", locale = "en")
    @Preview("ClearDataButton 2x", locale = "en", fontScale = 2.0f)
    @Preview("ClearDataButton RTL, 1x", locale = "he")
    @Preview("ClearDataButton RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(
        @PreviewParameter(ClearDataButtonPreviews::class) params: Params
    ) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
        val cleared = remember { mutableStateOf(params.isCleared) }
        NeevaTheme(useDarkTheme = params.darkTheme) {
            ClearDataButton(
                text = stringResource(R.string.debug_long_string_primary),
                cleared = cleared,
                onClick = {},
                rowModifier
            )
        }
    }
}
