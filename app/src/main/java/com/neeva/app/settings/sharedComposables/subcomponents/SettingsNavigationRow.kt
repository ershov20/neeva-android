package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsNavigationRow(title: String, onClick: () -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onClick() }
    ) {
        SettingsLabelText(text = title, modifier = Modifier.weight(1.0f))
    }
}

@Preview(name = "Settings Navigation Row, 1x font size", locale = "en")
@Preview(name = "Settings Navigation Row, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Navigation Row, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Navigation Row, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsNavigation_Preview() {
    NeevaTheme {
        SettingsNavigationRow(
            title = stringResource(R.string.debug_long_string_primary),
            onClick = {},
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview(name = "Settings Navigation Row Dark , 1x font size", locale = "en")
@Preview(name = "Settings Navigation Row Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Navigation Row Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Navigation Row Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsNavigation_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SettingsNavigationRow(
            title = stringResource(R.string.debug_long_string_primary),
            onClick = {},
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
