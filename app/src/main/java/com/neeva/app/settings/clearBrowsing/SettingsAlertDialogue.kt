package com.neeva.app.settings.clearBrowsing

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsAlertDialogue(
    text: String,
    confirmString: String,
    confirmAction: () -> Unit,
    dismissString: String,
    dismissAction: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismissAction,
        text = {
            Text(text = text)
        },
        confirmButton = {
            TextButton(
                onClick = confirmAction
            ) {
                Text(
                    text = confirmString,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = dismissAction
            ) {
                Text(
                    text = dismissString,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

class SettingsAlertDialoguePreviews :
    BooleanPreviewParameterProvider<SettingsAlertDialoguePreviews.Params>(1) {
    data class Params(
        val darkTheme: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0]
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
            SettingsAlertDialogue(
                text = stringResource(id = R.string.debug_long_string_primary),
                stringResource(id = R.string.confirm),
                {},
                stringResource(id = R.string.cancel),
                {}
            )
        }
    }
}
