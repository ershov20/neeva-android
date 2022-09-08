// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.sharedcomposables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.clearbrowsing.TimeClearingOption
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.RadioButtonGroup
import com.neeva.app.ui.widgets.RadioButtonItem

@Composable
fun SettingsDialog(
    @StringRes textId: Int,
    radioOptions: List<RadioButtonItem>? = null,
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

    @PortraitPreviews
    @Composable
    fun DefaultPreview(
        @PreviewParameter(SettingsDialogPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            var radioOptions: List<RadioButtonItem>? = null
            if (params.hasRadioOptions) {
                radioOptions = TimeClearingOption.values().map {
                    RadioButtonItem(title = it.string_id)
                }
            }

            val selectedOption = rememberSaveable { mutableStateOf(0) }

            SettingsDialog(
                textId = R.string.debug_long_string_primary,
                radioOptions = radioOptions,
                selectedOptionIndex = selectedOption,
                confirmStringId = android.R.string.ok,
                confirmAction = { run {} },
                dismissStringId = android.R.string.cancel,
                dismissAction = {}
            )
        }
    }
}
