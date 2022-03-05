package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsLabelRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    rowModifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
    ) {
        SettingsLabelText(
            primaryLabel = primaryLabel,
            secondaryLabel = secondaryLabel,
            enabled = enabled,
            columnModifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SettingsLabelText(
    primaryLabel: String,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    columnModifier: Modifier,
    textModifier: Modifier = Modifier
) {
    Column(columnModifier) {
        Text(
            text = primaryLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = textModifier.then(
                if (!enabled) {
                    textModifier.alpha(ContentAlpha.disabled)
                } else {
                    textModifier
                }
            )
        )
        if (secondaryLabel != null) {
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

class SettingsLabelRowPreviews :
    BooleanPreviewParameterProvider<SettingsLabelRowPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val isEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isEnabled = booleanArray[1]
    )

    @Preview("single label 1x scale", locale = "en")
    @Preview("single label 2x scale", locale = "en", fontScale = 2.0f)
    @Preview("single label RTL, 1x scale", locale = "he")
    @Preview("single label RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun SettingsNavigationRow_SingleLabel_Preview(
        @PreviewParameter(SettingsLabelRowPreviews::class) params: Params
    ) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
        NeevaTheme(useDarkTheme = params.darkTheme) {
            SettingsLabelRow(
                primaryLabel = stringResource(id = R.string.debug_long_string_primary),
                enabled = params.isEnabled,
                rowModifier = rowModifier
            )
        }
    }

    @Preview("double label 1x scale", locale = "en")
    @Preview("double label 2x scale", locale = "en", fontScale = 2.0f)
    @Preview("double label RTL, 1x scale", locale = "he")
    @Preview("double label RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun SettingsNavigationRow_DoubleLabel_Preview(
        @PreviewParameter(SettingsLabelRowPreviews::class) params: Params
    ) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
        NeevaTheme(useDarkTheme = params.darkTheme) {
            SettingsLabelRow(
                primaryLabel = stringResource(id = R.string.debug_long_string_primary),
                secondaryLabel = stringResource(id = R.string.debug_long_string_primary),
                enabled = params.isEnabled,
                rowModifier = rowModifier
            )
        }
    }
}
