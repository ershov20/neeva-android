package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsButtonRow(
    label: String,
    onClick: () -> Unit
) {
    BaseRowLayout(onTapRow = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Settings Button, 1x font size", locale = "en")
@Preview(name = "Settings Button, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Button, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Button, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsButton_Preview() {
    NeevaTheme {
        SettingsButtonRow(
            label = stringResource(R.string.debug_long_string_primary),
            onClick = {}
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
        SettingsButtonRow(
            label = stringResource(R.string.debug_long_string_primary),
            onClick = {}
        )
    }
}
