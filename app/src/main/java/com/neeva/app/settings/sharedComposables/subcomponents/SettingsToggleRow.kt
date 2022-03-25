package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.ui.NeevaSwitch
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun SettingsToggleRow(
    title: String,
    toggleState: MutableState<Boolean>,
    togglePrefKey: String,
    getTogglePreferenceSetter: (String?) -> ((Boolean) -> Unit)?,
    enabled: Boolean = true,
    modifier: Modifier
) {
    NeevaSwitch(
        switchLabelContent = {
            SettingsLabelText(title, enabled = enabled, columnModifier = it)
        },
        isEnabled = enabled,
        isChecked = toggleState.value,
        onCheckedChange = { getTogglePreferenceSetter(togglePrefKey)?.invoke(it) },
        modifier = modifier
    )
}

@Preview("1x scale", locale = "en")
@Preview("2x scale", locale = "en", fontScale = 2.0f)
@Preview("RTL, 1x scale", locale = "he")
@Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
private fun SettingsToggleRowPreview() {
    OneBooleanPreviewContainer { isEnabled ->
        val toggleState = remember { mutableStateOf(true) }
        SettingsToggleRow(
            title = stringResource(R.string.debug_long_string_primary),
            enabled = isEnabled,
            toggleState = toggleState,
            togglePrefKey = "",
            getTogglePreferenceSetter = { {} },
            modifier = SettingsUIConstants
                .rowModifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}
