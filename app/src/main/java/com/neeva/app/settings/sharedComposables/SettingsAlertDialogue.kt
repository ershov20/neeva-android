package com.neeva.app.settings.sharedComposables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.R
import com.neeva.app.settings.clearBrowsing.TimeClearingOption
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsDialog(
    @StringRes textId: Int,
    radioOptions: List<Int>? = null,
    @StringRes confirmStringId: Int,
    confirmAction: (Int?) -> Unit,
    @StringRes dismissStringId: Int,
    dismissAction: () -> Unit,
) {
    val selectedOption = rememberSaveable { mutableStateOf(radioOptions?.first()) }
    AlertDialog(
        onDismissRequest = dismissAction,
        text = {
            Column {
                Text(text = stringResource(textId))
                if (radioOptions != null && radioOptions.isNotEmpty()) {
                    RadioButtonGroup(
                        radioOptions,
                        selectedOption.value,
                        onSelect = { selectId -> { selectedOption.value = selectId } }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirmAction(selectedOption.value) }) {
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
    radioOptions: List<Int>,
    selectedOption: Int?,
    onSelect: (Int) -> (() -> Unit)
) {
    Column {
        radioOptions.forEach { textId ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(
                        selected = (textId == selectedOption),
                        onClick = onSelect(textId)
                    )
                    .fillMaxWidth()
            ) {
                RadioButton(
                    selected = (textId == selectedOption),
                    onClick = onSelect(textId)
                )
                Text(text = stringResource(id = textId))
            }
        }
    }
}

class SettingsAlertDialoguePreviews :
    BooleanPreviewParameterProvider<SettingsAlertDialoguePreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val hasRadioOptions: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        hasRadioOptions = booleanArray[1]
    )

    @Preview("SettingsAlertDialogue 1x", locale = "en")
    @Preview("SettingsAlertDialogue 2x", locale = "en", fontScale = 2.0f)
    @Preview("SettingsAlertDialogue RTL, 1x", locale = "he")
    @Preview("SettingsAlertDialogue RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(
        @PreviewParameter(SettingsAlertDialoguePreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            var radioOptions: List<Int>? = null
            if (params.hasRadioOptions) {
                radioOptions = TimeClearingOption.values().map { it.string_id }
            }

            SettingsDialog(
                textId = R.string.debug_long_string_primary,
                radioOptions = radioOptions,
                confirmStringId = R.string.confirm,
                confirmAction = { {} },
                dismissStringId = R.string.cancel,
                dismissAction = {}
            )
        }
    }
}
