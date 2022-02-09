package com.neeva.app.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.NeevaBrowser
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsButtonRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsLabel
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsLinkRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsNavigationRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsToggleRow
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsRow(
    rowData: SettingsRowData,
    settingsViewModel: SettingsViewModel,
    onClick: () -> Unit = {},
    modifier: Modifier
) {
    var title = stringResource(rowData.titleId)
    val versionString = NeevaBrowser.versionString
    if (rowData.titleId == R.string.settings_neeva_browser_version && versionString != null) {
        title = stringResource(rowData.titleId, versionString)
    }

    val toggleState = settingsViewModel.getToggleState(rowData.togglePreferenceKey)
    when (rowData.type) {
        SettingsRowType.BUTTON -> {
            SettingsButtonRow(title, onClick, modifier)
        }
        SettingsRowType.LABEL -> {
            SettingsLabel(title, modifier)
        }
        SettingsRowType.LINK -> {
            if (rowData.url != null) {
                SettingsLinkRow(title, settingsViewModel::openUrl, rowData.url, modifier)
            }
        }
        SettingsRowType.TOGGLE -> {
            if (toggleState != null && rowData.togglePreferenceKey != null) {
                SettingsToggleRow(
                    title = title,
                    toggleState = toggleState,
                    togglePrefKey = rowData.togglePreferenceKey,
                    getTogglePreferenceSetter = settingsViewModel::getTogglePreferenceSetter,
                    enabled = rowData.enabled,
                    modifier = modifier
                )
            }
        }
        SettingsRowType.NAVIGATION -> {
            SettingsNavigationRow(title = title, onClick = onClick, modifier = modifier)
        }
        SettingsRowType.PROFILE -> {
            if (settingsViewModel.isSignedOut()) {
                SettingsButtonRow(
                    title = stringResource(R.string.settings_sign_in_to_join_neeva),
                    onClick = onClick,
                    modifier = modifier
                )
            } else {
                ProfileRow(onClick = onClick, modifier = modifier)
            }
        }
    }
}

@Preview(name = "Toggle, 1x font size", locale = "en")
@Preview(name = "Toggle, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewToggle() {
    NeevaTheme {
        SettingsRow(
            rowData = SettingsRowData(
                SettingsRowType.TOGGLE,
                R.string.debug_long_string_primary
            ),
            settingsViewModel = getFakeSettingsViewModel(),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
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
            rowData = SettingsRowData(
                SettingsRowType.LINK,
                R.string.debug_long_string_primary,
                Uri.parse(""),
                togglePreferenceKey = ""
            ),
            settingsViewModel = getFakeSettingsViewModel(),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewLabel() {
    NeevaTheme {
        SettingsRow(
            rowData = SettingsRowData(
                SettingsRowType.LABEL,
                R.string.debug_long_string_primary
            ),
            settingsViewModel = getFakeSettingsViewModel(),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
