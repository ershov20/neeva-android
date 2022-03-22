package com.neeva.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.theme.getClickableAlpha

@Composable
fun NeevaSwitch(
    switchLabelContent: @Composable (Modifier) -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .clickable(isEnabled) { onCheckedChange(!isChecked) }
            .alpha(getClickableAlpha(isEnabled))
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            switchLabelContent(Modifier.weight(1f))

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
                onCheckedChange = null
            )
        }
    }
}

@Composable
fun NeevaSwitch(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    NeevaSwitch(
        switchLabelContent = {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = it
            )
        },
        isEnabled = isEnabled,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
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
            title = "Some random setting that the user can toggle",
            isChecked = isCheckedState.value,
            onCheckedChange = { isCheckedState.value = it },
            isEnabled = isEnabled
        )
    }
}
