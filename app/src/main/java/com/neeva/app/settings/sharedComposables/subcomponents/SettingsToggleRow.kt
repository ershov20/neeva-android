package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.getClickableAlpha

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
        SettingsLabelText(title, enabled = enabled, columnModifier = Modifier.weight(1.0f))
        Switch(
            enabled = enabled,
            checked = toggleState.value,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                disabledCheckedThumbColor = MaterialTheme.colorScheme.inverseOnSurface,
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.inverseOnSurface,
                disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface,
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface,
            ),
            onCheckedChange = getTogglePreferenceSetter(togglePrefKey),
            modifier = Modifier.size(48.dp).alpha(getClickableAlpha(enabled))
        )
    }
}

class SettingsToggleRowPreviews :
    BooleanPreviewParameterProvider<SettingsToggleRowPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val isEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isEnabled = booleanArray[1]
    )

    @Preview("1x scale", locale = "en")
    @Preview("2x scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x scale", locale = "he")
    @Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun SettingsToggleRow_Preview(
        @PreviewParameter(SettingsToggleRowPreviews::class) params: Params
    ) {
        var toggleState = remember { mutableStateOf(true) }
        NeevaTheme(useDarkTheme = params.darkTheme) {
            SettingsToggleRow(
                title = stringResource(R.string.debug_long_string_primary),
                enabled = params.isEnabled,
                toggleState = toggleState,
                togglePrefKey = "",
                getTogglePreferenceSetter = { {} },
                modifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
