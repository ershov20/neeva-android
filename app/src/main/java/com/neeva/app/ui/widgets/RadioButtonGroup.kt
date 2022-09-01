// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LandscapePreviewsDark
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark

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

@PortraitPreviews
@LandscapePreviews
@Composable
fun RadioButtonGroup_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        RadioButtonGroup(
            radioOptions = listOf(
                "Option 1",
                stringResource(id = R.string.debug_long_string_primary),
                "Option 3",
                "Option 4",
                "Option 5"
            ),
            selectedOptionIndex = 2,
            onSelect = {}
        )
    }
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun RadioButtonGroup_PreviewDark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        Surface {
            RadioButtonGroup(
                radioOptions = listOf(
                    "Option 1",
                    stringResource(id = R.string.debug_long_string_primary),
                    "Option 3",
                    "Option 4",
                    "Option 5"
                ),
                selectedOptionIndex = 2,
                onSelect = {}
            )
        }
    }
}
