package com.neeva.app.spaces

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalSpaceStore
import com.neeva.app.R
import com.neeva.app.ui.widgets.ComposeTextFieldWorkaround

@Composable
fun CreateSpaceDialog(
    isDialogVisible: State<Boolean>,
    promptToOpenSpace: Boolean,
    onDismissRequested: () -> Unit
) {
    if (!isDialogVisible.value) return

    val spaceStore = LocalSpaceStore.current
    val appNavModel = LocalAppNavModel.current
    val spaceName = remember { mutableStateOf("") }
    val isPrimaryButtonEnabled = remember { mutableStateOf(false) }
    val isDismissing = remember { mutableStateOf(false) }

    val onDone = {
        if (spaceName.value.isNotBlank()) {
            spaceStore.createSpace(
                spaceName = spaceName.value,
                promptToOpenSpace = promptToOpenSpace
            ) { spaceId ->
                appNavModel.showSpaceDetail(spaceId)
            }
            isDismissing.value = true
        }
    }

    val onCancel = {
        isDismissing.value = true
    }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                enabled = isPrimaryButtonEnabled.value,
                onClick = onDone
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        title = {
            Text(stringResource(R.string.space_create))
        },
        text = {
            ComposeTextFieldWorkaround(
                isDismissing = isDismissing.value,
                onDismissRequested = onDismissRequested
            ) { focusRequester ->
                OutlinedTextField(
                    value = spaceName.value,
                    onValueChange = {
                        spaceName.value = it
                        isPrimaryButtonEnabled.value = it.isNotBlank()
                    },
                    singleLine = true,
                    placeholder = {
                        Text(stringResource(R.string.space_create_placeholder))
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    keyboardActions = KeyboardActions(
                        onDone = { onDone() }
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        }
    )
}
