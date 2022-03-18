package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun SettingsNavigationRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .then(modifier)
    ) {
        SettingsLabelText(
            primaryLabel = primaryLabel,
            secondaryLabel = secondaryLabel,
            enabled = enabled,
            columnModifier = Modifier.weight(1.0f)
        )
        Image(
            painter = painterResource(R.drawable.ic_navigate_next),
            contentDescription = primaryLabel,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Preview("SettingsNavigationRow, LTR, 1x scale", locale = "en")
@Preview("SettingsNavigationRow, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("SettingsNavigationRow, RTL, 1x scale", locale = "he")
@Composable
fun SettingsNavigationRowPreview() {
    OneBooleanPreviewContainer { isEnabled ->
        SettingsNavigationRow(
            primaryLabel = stringResource(R.string.debug_long_string_primary),
            enabled = isEnabled,
            onClick = {},
            modifier = SettingsUIConstants
                .rowModifier
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
