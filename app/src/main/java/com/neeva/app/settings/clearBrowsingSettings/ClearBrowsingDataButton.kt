package com.neeva.app.settings.clearBrowsingSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ClearBrowsingDataButton(
    text: String,
    clearHistory: () -> Unit,
    modifier: Modifier
) {
    val textState = remember { mutableStateOf(text) }
    val clearText = stringResource(id = R.string.settings_selected_data_cleared_success)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.then(
            if (textState.value == clearText) {
                Modifier
            } else {
                Modifier.clickable {
                    clearHistory()
                    textState.value = clearText
                }
            }
        )
    ) {
        Text(
            text = textState.value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "ClearBrowsingDataButton, 1x font size", locale = "en")
@Preview(name = "ClearBrowsingDataButton, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "ClearBrowsingDataButton, RTL, 1x font size", locale = "he")
@Preview(name = "ClearBrowsingDataButton, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingDataButton_Preview() {
    val rowModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .padding(16.dp)
        .background(MaterialTheme.colorScheme.surface)

    NeevaTheme {
        ClearBrowsingDataButton(
            text = stringResource(R.string.debug_long_string_primary),
            clearHistory = {},
            rowModifier
        )
    }
}

@Preview(name = "ClearBrowsingDataButton Dark, 1x font size", locale = "en")
@Preview(name = "ClearBrowsingDataButton Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "ClearBrowsingDataButton Dark, RTL, 1x font size", locale = "he")
@Preview(name = "ClearBrowsingDataButton Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun ClearBrowsingDataButton_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)

        ClearBrowsingDataButton(
            text = stringResource(R.string.debug_long_string_primary),
            clearHistory = {},
            rowModifier
        )
    }
}
