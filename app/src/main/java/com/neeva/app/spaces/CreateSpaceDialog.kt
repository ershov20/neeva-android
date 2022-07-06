package com.neeva.app.spaces

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalSpaceStore
import com.neeva.app.R
import com.neeva.app.ui.KeyboardFocusEffect

@Composable
fun CreateSpaceDialog(
    isDialogVisible: State<Boolean>,
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
            spaceStore.createSpace(spaceName.value) { spaceId ->
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
            if (!isDismissing.value) {
                val focusRequester = remember { FocusRequester() }
                KeyboardFocusEffect(focusRequester)

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
            } else {
                // Super dirty hack to work around https://issuetracker.google.com/issues/230536793
                // Compose has a bug that prevents the keyboard from being dismissed correctly when
                // the parent of a TextField is removed.  Because the OutlinedTextField is contained
                // as part of the AlertDialog (which normally disappears all at once), we hit it.
                // Oddly, this manifests as the keyboard disappearing and then reappearing as
                // _something_ that is no longer on screen requests focus again.
                // To get around this, we start the dialog dismissal by removing the TextField from
                // the composition (which hides the keyboard), then remove the dialog itself.
                LaunchedEffect(true) {
                    onDismissRequested()
                }
            }
        }
    )
}
