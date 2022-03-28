package com.neeva.app.settings.sharedComposables

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.BuildConfig
import com.neeva.app.R
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.clearBrowsing.ClearDataButtonView
import com.neeva.app.settings.mockSettingsViewModel
import com.neeva.app.settings.profile.ProfileRowContainer
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultBrowserRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsButtonRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsLabelRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsLinkRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsNavigationRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsToggleRow
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.userdata.NeevaUser

@Composable
fun SettingsRow(
    rowData: SettingsRowData,
    settingsViewModel: SettingsViewModel,
    onClick: (() -> Unit)? = null,
    modifier: Modifier
) {
    var title = stringResource(rowData.titleId)
    val versionString = BuildConfig.VERSION_NAME
    if (rowData.titleId == R.string.settings_neeva_browser_version) {
        title = stringResource(rowData.titleId, versionString)
    }

    val toggleState = settingsViewModel.getToggleState(rowData.togglePreferenceKey)

    when (rowData.type) {
        SettingsRowType.BUTTON -> {
            if (rowData.titleId == R.string.settings_sign_out) {
                SettingsButtonRow(title, settingsViewModel::signOut, modifier)
            } else {
                onClick?.let { SettingsButtonRow(title, it, modifier) }
            }
        }

        SettingsRowType.LABEL -> {
            SettingsLabelRow(primaryLabel = title, rowModifier = modifier)
        }

        SettingsRowType.LINK -> {
            if (rowData.url != null) {
                SettingsLinkRow(
                    title,
                    { settingsViewModel.openUrl(rowData.url, rowData.openUrlViaIntent) },
                    modifier
                )
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
            if (onClick != null) {
                // TODO(kobec): discuss with Dan to figure out a better way to deal with special cases like Set Android Default Browser
                // https://github.com/neevaco/neeva-android/pull/376#discussion_r816329896
                if (rowData.titleId == R.string.settings_default_browser) {
                    SetDefaultBrowserRow(
                        settingsViewModel.getSetDefaultAndroidBrowserManager(),
                        navigateToPane = onClick,
                        rowModifier = modifier
                    )
                } else {
                    SettingsNavigationRow(
                        primaryLabel = title,
                        enabled = rowData.enabled,
                        onClick = onClick,
                        modifier = modifier
                    )
                }
            }
        }

        SettingsRowType.PROFILE -> {
            ProfileRowContainer(
                isSignedOut = settingsViewModel.isSignedOut(),
                showSSOProviderAsPrimaryLabel = rowData.showSSOProviderAsPrimaryLabel,
                userData = settingsViewModel.getNeevaUserData(),
                onClick = onClick,
                modifier = modifier
            )
        }

        SettingsRowType.CLEAR_DATA_BUTTON -> {
            ClearDataButtonView(
                getToggleState = settingsViewModel::getToggleState,
                rowData = rowData,
                onClearBrowsingData = settingsViewModel::clearBrowsingData,
                rowModifier = modifier
            )
        }
    }
}

fun getFormattedSSOProviderName(ssoProvider: NeevaUser.SSOProvider): String {
    return when (ssoProvider) {
        NeevaUser.SSOProvider.GOOGLE -> "Google"
        NeevaUser.SSOProvider.MICROSOFT -> "Microsoft"
        NeevaUser.SSOProvider.OKTA -> "Okta"
        NeevaUser.SSOProvider.APPLE -> "Apple"
        NeevaUser.SSOProvider.UNKNOWN -> "Unknown"
    }
}

@Preview(name = "Toggle, LTR, 1x font size", locale = "en")
@Preview(name = "Toggle, LTR, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Toggle, RTL, 1x font size", locale = "he")
@Preview(name = "Toggle, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewToggle() {
    LightDarkPreviewContainer {
        SettingsRow(
            rowData = SettingsRowData(
                SettingsRowType.TOGGLE,
                R.string.debug_long_string_primary,
                togglePreferenceKey = "toggle preference key"
            ),
            settingsViewModel = mockSettingsViewModel,
            modifier = SettingsUIConstants.rowModifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview(name = "Link, 1x font size", locale = "en")
@Preview(name = "Link, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Link, RTL, 1x font size", locale = "he")
@Preview(name = "Link, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewLink() {
    LightDarkPreviewContainer {
        SettingsRow(
            rowData = SettingsRowData(
                SettingsRowType.LINK,
                R.string.debug_long_string_primary,
                Uri.parse(""),
                togglePreferenceKey = ""
            ),
            settingsViewModel = mockSettingsViewModel,
            modifier = SettingsUIConstants.rowModifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewLabel() {
    LightDarkPreviewContainer {
        SettingsRow(
            rowData = SettingsRowData(
                SettingsRowType.LABEL,
                R.string.debug_long_string_primary
            ),
            settingsViewModel = mockSettingsViewModel,
            modifier = SettingsUIConstants.rowModifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}
