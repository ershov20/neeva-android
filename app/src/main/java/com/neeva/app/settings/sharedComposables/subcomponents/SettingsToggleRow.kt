package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsToggleRow(
    title: String,
    toggleState: MutableState<Boolean>,
    togglePrefKey: String,
    getTogglePreferenceSetter: (String?) -> ((Boolean) -> Unit)?,
    enabled: Boolean = true,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        SettingsLabelText(title, modifier = Modifier.weight(1.0f))
        Switch(
            enabled = enabled,
            checked = toggleState.value,
            modifier = Modifier.size(48.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                uncheckedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            ),
            onCheckedChange = getTogglePreferenceSetter(togglePrefKey)
        )
    }
}

@Preview(name = "Settings Toggle, 1x font size", locale = "en")
@Preview(name = "Settings Toggle, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Toggle, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Toggle, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsToggleRow_Preview() {
    var toggleState = remember { mutableStateOf(true) }
    NeevaTheme {
        SettingsToggleRow(
            title = stringResource(R.string.debug_long_string_primary),
            toggleState = toggleState,
            togglePrefKey = "",
            getTogglePreferenceSetter = { {} },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview(name = "Settings Toggle Dark, 1x font size", locale = "en")
@Preview(name = "Settings Toggle Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Toggle Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Toggle Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsToggleRow_Dark_Preview() {
    var toggleState = remember { mutableStateOf(true) }
    NeevaTheme(useDarkTheme = true) {
        SettingsToggleRow(
            title = stringResource(R.string.debug_long_string_primary),
            toggleState = toggleState,
            togglePrefKey = "",
            getTogglePreferenceSetter = { {} },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
