package com.neeva.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun ConfirmationAlertDialog(
    title: String?,
    message: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    assert(title != null || message != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        title = title?.let {
            { Text(text = it) }
        },
        text = message?.let {
            { Text(text = it) }
        }
    )
}
