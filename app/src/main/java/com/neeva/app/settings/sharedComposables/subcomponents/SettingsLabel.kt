package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsLabel(
    title: String,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        SettingsLabelText(title)
    }
}

@Composable
fun SettingsLabelText(text: String, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.then(
            if (!enabled) {
                modifier.alpha(ContentAlpha.disabled)
            } else {
                modifier
            }
        )
    )
}

@Preview(name = "Settings Label, 1x font size", locale = "en")
@Preview(name = "Settings Label, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Label, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Label, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsLabel_Preview() {
    val rowModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .padding(16.dp)
        .background(MaterialTheme.colorScheme.surface)

    NeevaTheme {
        SettingsLabel(
            title = stringResource(id = R.string.debug_long_string_primary),
            rowModifier
        )
    }
}

@Preview(name = "Settings Label Dark, 1x font size", locale = "en")
@Preview(name = "Settings Label Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Label Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Label Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsLabel_Dark_Preview() {
    val rowModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .padding(16.dp)
        .background(MaterialTheme.colorScheme.surface)
    NeevaTheme(useDarkTheme = true) {
        SettingsLabel(title = stringResource(id = R.string.debug_long_string_primary), rowModifier)
    }
}
