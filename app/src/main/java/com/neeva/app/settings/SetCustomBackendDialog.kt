package com.neeva.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.AnnotatedSpannable
import com.neeva.app.ui.widgets.ComposeTextFieldWorkaround

@Composable
fun SetCustomBackendDialog(
    onShowSnackbar: (String) -> Unit,
    onDismissRequested: () -> Unit
) {
    var isDismissing by remember { mutableStateOf(false) }
    val startDismissing = { isDismissing = true }

    val sharedPreferencesModel = LocalSharedPreferencesModel.current
    val currentDomain = SharedPrefFolder.App.CustomNeevaDomain.get(sharedPreferencesModel)
    var input by remember { mutableStateOf(currentDomain) }

    AlertDialog(
        onDismissRequest = startDismissing,
        confirmButton = {
            val restartString = stringResource(R.string.settings_debug_restart)
            TextButton(
                onClick = {
                    SharedPrefFolder.App.CustomNeevaDomain.set(
                        sharedPreferencesModel,
                        input
                    )
                    startDismissing()
                    onShowSnackbar(restartString)
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = startDismissing) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        title = {
            Text(stringResource(id = R.string.settings_debug_custom_neeva_domain))
        },
        text = {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_LARGE),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnnotatedSpannable(
                    rawHtml = stringResource(
                        R.string.settings_debug_custom_neeva_domain_description
                    )
                )

                ComposeTextFieldWorkaround(
                    isDismissing = isDismissing,
                    onDismissRequested = onDismissRequested
                ) { focusRequester ->
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }
        }
    )
}
