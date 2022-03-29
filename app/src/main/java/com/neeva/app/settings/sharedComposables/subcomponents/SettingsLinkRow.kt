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
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsLinkRow(
    title: String,
    openUrl: () -> Unit,
    rowModifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { openUrl() }.then(rowModifier)
    ) {
        SettingsLabelText(primaryLabel = title, columnModifier = Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_baseline_open_in_new_24),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Preview(name = "Settings Link Row, 1x font size", locale = "en")
@Preview(name = "Settings Link Row, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Link Row, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Link Row, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsLinkRow_Preview() {
    NeevaTheme {
        SettingsLinkRow(
            title = "A Label",
            openUrl = {},
            SettingsUIConstants.rowModifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview(name = "Settings Link Row Dark, 1x font size", locale = "en")
@Preview(name = "Settings Link Row Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Link Row Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Link Row Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsLinkRow_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SettingsLinkRow(
            title = "A Label",
            openUrl = {},
            SettingsUIConstants.rowModifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}
