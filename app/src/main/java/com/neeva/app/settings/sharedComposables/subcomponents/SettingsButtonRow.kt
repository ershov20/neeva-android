package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsButtonRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }.then(modifier)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Settings Button, 1x font size", locale = "en")
@Preview(name = "Settings Button, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Button, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Button, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsButton_Preview() {
    val rowModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .padding(16.dp)
        .background(MaterialTheme.colorScheme.surface)

    NeevaTheme {
        SettingsButtonRow(
            title = stringResource(R.string.debug_long_string_primary),
            onClick = {},
            rowModifier
        )
    }
}

@Preview(name = "Settings Button Dark, 1x font size", locale = "en")
@Preview(name = "Settings Button Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Button Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Button Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsButton_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)

        SettingsButtonRow(
            title = stringResource(R.string.debug_long_string_primary),
            onClick = {},
            rowModifier
        )
    }
}
