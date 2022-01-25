package com.neeva.app.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.NeevaBrowser
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsRow(
    data: SettingsRowData,
    openUrl: (Uri) -> Unit,
    onClearHistory: () -> Unit,
    getTogglePreferenceSetter: (String?) -> ((Boolean) -> Unit)?,
    getToggleState: (String?) -> MutableState<Boolean>?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (data.type == SettingsRowType.LINK && data.url != null) {
                    Modifier.clickable { openUrl(data.url) }
                } else if (
                    data.type == SettingsRowType.NAVIGATION &&
                    data.title_id == R.string.settings_clear_browsing_data
                ) {
                    Modifier.clickable { onClearHistory() }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        var title = data.title_id?.let { stringResource(it) }
        val versionString = NeevaBrowser.versionString
        if (data.title_id == R.string.settings_neeva_browser_version && versionString != null) {
            title = stringResource(data.title_id, versionString)
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.0f)
            )
            val toggleState = getToggleState(data.togglePreferenceKey)

            if (data.type == SettingsRowType.LINK) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_open_in_new_24),
                    contentDescription = title,
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.size(48.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else if (data.type == SettingsRowType.TOGGLE && toggleState != null) {
                Switch(
                    checked = toggleState.value,
                    modifier = Modifier.size(48.dp),
                    onCheckedChange = getTogglePreferenceSetter(data.togglePreferenceKey)
                )
            }
        }
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewToggle() {
    NeevaTheme {
        SettingsRow(
            data = SettingsRowData(
                SettingsRowType.TOGGLE,
                R.string.debug_long_string_primary
            ),
            openUrl = {},
            onClearHistory = {},
            getTogglePreferenceSetter = { {} },
            getToggleState = { mutableStateOf(true) }
        )
    }
}

@Preview(name = "Link, 1x font size", locale = "en")
@Preview(name = "Link, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Link, RTL, 1x font size", locale = "he")
@Preview(name = "Link, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewLink() {
    NeevaTheme {
        SettingsRow(
            data = SettingsRowData(
                SettingsRowType.LINK,
                R.string.debug_long_string_primary,
                Uri.parse("")
            ),
            openUrl = {},
            onClearHistory = {},
            getTogglePreferenceSetter = { {} },
            getToggleState = { mutableStateOf(true) }
        )
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewLabel() {
    NeevaTheme {
        SettingsRow(
            data = SettingsRowData(
                SettingsRowType.LABEL,
                R.string.debug_long_string_primary
            ),
            openUrl = {},
            onClearHistory = {},
            getTogglePreferenceSetter = { {} },
            getToggleState = { mutableStateOf(true) }
        )
    }
}
