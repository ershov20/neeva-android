package com.neeva.app.settings.sharedComposables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.clearBrowsing.TimeClearingOption
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsDialog(
    @StringRes textId: Int,
    radioOptions: List<String>? = null,
    selectedOptionIndex: MutableState<Int>,
    saveSelectedOptionIndex: (Int) -> Unit = {},
    @StringRes confirmStringId: Int,
    confirmAction: (Int?) -> Unit,
    @StringRes dismissStringId: Int,
    dismissAction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = dismissAction,
        text = {
            Column {
                Text(text = stringResource(textId))
                Spacer(Modifier.height(24.dp))
                RadioButtonGroup(
                    radioOptions,
                    selectedOptionIndex.value,
                    onSelect = { index ->
                        selectedOptionIndex.value = index
                        saveSelectedOptionIndex(index)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { confirmAction(selectedOptionIndex.value) }) {
                Text(
                    text = stringResource(confirmStringId),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = dismissAction) {
                Text(
                    text = stringResource(dismissStringId),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioButtonGroup(
    radioOptions: List<String>?,
    selectedOptionIndex: Int,
    onSelect: (Int) -> Unit
) {
    if (radioOptions != null && radioOptions.isNotEmpty()) {
        Column {
            radioOptions.forEachIndexed { index, _ ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = (index == selectedOptionIndex),
                            onClick = { onSelect(index) }
                        )
                        .fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (index == selectedOptionIndex),
                        onClick = { onSelect(index) }
                    )
                    Text(text = radioOptions[index])
                }
            }
        }
    }
}

class SettingsDialogPreviews :
    BooleanPreviewParameterProvider<SettingsDialogPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val hasRadioOptions: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        hasRadioOptions = booleanArray[1]
    )

    @Preview("Settings Dialog 1x", locale = "en")
    @Preview("Settings Dialog 2x", locale = "en", fontScale = 2.0f)
    @Preview("Settings Dialog RTL, 1x", locale = "he")
    @Composable
    fun DefaultPreview(
        @PreviewParameter(SettingsDialogPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            var radioOptions: List<String>? = null
            if (params.hasRadioOptions) {
                radioOptions = TimeClearingOption.values().map { stringResource(it.string_id) }
            }

            val selectedOption = rememberSaveable { mutableStateOf(0) }

            SettingsDialog(
                textId = R.string.debug_long_string_primary,
                radioOptions = radioOptions,
                selectedOptionIndex = selectedOption,
                confirmStringId = R.string.confirm,
                confirmAction = { run {} },
                dismissStringId = R.string.cancel,
                dismissAction = {}
            )
        }
    }
}
