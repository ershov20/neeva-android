// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import com.neeva.app.ui.theme.Dimensions

data class RadioButtonItem(
    @StringRes val title: Int,
    @StringRes val description: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioButtonGroup(
    radioOptions: List<RadioButtonItem>?,
    selectedOptionIndex: Int,
    addAdditionalHorizontalPadding: Boolean = false,
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
                        .then(
                            if (radioOptions[index].description != null) {
                                Modifier.padding(vertical = Dimensions.PADDING_SMALL)
                            } else {
                                Modifier
                            }
                        )
                        .fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (index == selectedOptionIndex),
                        onClick = { onSelect(index) },
                        modifier = if (addAdditionalHorizontalPadding) {
                            Modifier.padding(horizontal = Dimensions.PADDING_MEDIUM)
                        } else {
                            Modifier
                        }
                    )
                    radioOptions[index].apply {
                        StackedText(
                            primaryLabel = stringResource(title),
                            secondaryLabel = description?.let { stringResource(it) },
                            primaryMaxLines = 2,
                            secondaryMaxLines = 2,
                            modifier = Modifier.padding(end = Dimensions.PADDING_LARGE)
                        )
                    }
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
                RadioButtonItem(
                    R.string.debug_short_string_primary,
                    R.string.debug_short_string_secondary
                ),
                RadioButtonItem(
                    R.string.debug_long_string_primary,
                    R.string.debug_short_string_secondary
                ),
                RadioButtonItem(
                    R.string.debug_short_string_primary,
                    R.string.debug_long_string_secondary
                ),
                RadioButtonItem(
                    R.string.debug_long_string_primary,
                    R.string.debug_long_string_secondary
                ),
                RadioButtonItem(
                    R.string.debug_short_string_primary,
                    null
                )
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
                    RadioButtonItem(
                        R.string.debug_short_string_primary,
                        R.string.debug_short_string_secondary
                    ),
                    RadioButtonItem(
                        R.string.debug_long_string_primary,
                        R.string.debug_short_string_secondary
                    ),
                    RadioButtonItem(
                        R.string.debug_short_string_primary,
                        R.string.debug_long_string_secondary
                    ),
                    RadioButtonItem(
                        R.string.debug_long_string_primary,
                        R.string.debug_long_string_secondary
                    ),
                    RadioButtonItem(
                        R.string.debug_short_string_primary,
                        null
                    )
                ),
                selectedOptionIndex = 2,
                onSelect = {}
            )
        }
    }
}
