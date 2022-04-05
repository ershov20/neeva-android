package com.neeva.app.ui

import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
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
                primaryLabel = primaryLabel, secondaryLabel = secondaryLabel,
                maxLines = 2,
                enabled = enabled
            )
        },
        isEnabled = enabled,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange
    )
}

@Preview("NeevaSwitch LTR 1x font scale", locale = "en")
@Preview("NeevaSwitch LTR 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("NeevaSwitch RTL 1x font scale", locale = "he")
@Composable
fun NeevaSwitchPreview() {
    TwoBooleanPreviewContainer { isChecked, isEnabled ->
        val isCheckedState = remember { mutableStateOf(isChecked) }
        NeevaSwitch(
            primaryLabel = "Some random setting that the user can toggle",
            isChecked = isCheckedState.value,
            onCheckedChange = { isCheckedState.value = it },
            enabled = isEnabled,
            secondaryLabel = "Some secondary label of the setting toggle."
        )
    }
}
