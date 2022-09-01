// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.getClickableAlpha
import com.neeva.app.ui.widgets.StackedText

@Composable
fun NeevaSwitch(
    switchLabelContent: @Composable () -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true
) {
    BaseRowLayout(
        onTapRow = { if (isEnabled) onCheckedChange(!isChecked) },
        endComposable = {
            Switch(
                enabled = isEnabled,
                checked = isChecked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.inverseOnSurface,
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.inverseOnSurface,
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface,
                    disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface,
                ),
                onCheckedChange = onCheckedChange
            )
        },
        endComposablePadding = Dimensions.PADDING_LARGE,
        modifier = Modifier.alpha(getClickableAlpha(isEnabled))
    ) {
        switchLabelContent()
    }
}

@Composable
fun NeevaSwitch(
    primaryLabel: String,
    secondaryLabel: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    NeevaSwitch(
        switchLabelContent = {
            StackedText(
                primaryLabel = primaryLabel,
                secondaryLabel = secondaryLabel,
                primaryMaxLines = Int.MAX_VALUE,
                secondaryMaxLines = Int.MAX_VALUE,
                enabled = enabled
            )
        },
        isEnabled = enabled,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange
    )
}

@PortraitPreviews
@Composable
fun NeevaSwitchPreviewEnabled() {
    LightDarkPreviewContainer {
        val isCheckedState = remember { mutableStateOf(true) }
        NeevaSwitch(
            primaryLabel = "Some random setting that the user can toggle",
            isChecked = isCheckedState.value,
            onCheckedChange = { isCheckedState.value = it },
            enabled = true,
            secondaryLabel = "Some secondary label of the setting toggle."
        )
    }
}

@PortraitPreviews
@Composable
fun NeevaSwitchPreviewEnabled_NotChecked() {
    LightDarkPreviewContainer {
        val isCheckedState = remember { mutableStateOf(false) }
        NeevaSwitch(
            primaryLabel = "Some random setting that the user can toggle",
            isChecked = isCheckedState.value,
            onCheckedChange = { isCheckedState.value = it },
            enabled = true,
            secondaryLabel = "Some secondary label of the setting toggle."
        )
    }
}

@PortraitPreviews
@Composable
fun NeevaSwitchPreviewDisabled() {
    LightDarkPreviewContainer {
        val isCheckedState = remember { mutableStateOf(true) }
        NeevaSwitch(
            primaryLabel = "Some random setting that the user can toggle",
            isChecked = isCheckedState.value,
            onCheckedChange = { isCheckedState.value = it },
            enabled = false,
            secondaryLabel = "Some secondary label of the setting toggle."
        )
    }
}

@PortraitPreviews
@Composable
fun NeevaSwitchPreviewDisabled_NotChecked() {
    LightDarkPreviewContainer {
        val isCheckedState = remember { mutableStateOf(false) }
        NeevaSwitch(
            primaryLabel = "Some random setting that the user can toggle",
            isChecked = isCheckedState.value,
            onCheckedChange = { isCheckedState.value = it },
            enabled = false,
            secondaryLabel = "Some secondary label of the setting toggle."
        )
    }
}
