package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.getClickableAlpha

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
            modifier = textModifier.alpha(getClickableAlpha(enabled))
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

@Preview("SettingsLabel, LTR, 1x scale", locale = "en")
@Preview("SettingsLabel, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("SettingsLabel, RTL, 1x scale", locale = "he")
@Preview("SettingsLabel, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
private fun SettingsLabelRowPreview() {
    TwoBooleanPreviewContainer { isEnabled, showSecondaryLabel ->
        SettingsLabelRow(
            primaryLabel = stringResource(id = R.string.debug_long_string_primary),
            secondaryLabel = if (showSecondaryLabel) {
                stringResource(R.string.debug_long_string_primary)
            } else {
                null
            },
            enabled = isEnabled,
            rowModifier = SettingsUIConstants
                .rowModifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}
