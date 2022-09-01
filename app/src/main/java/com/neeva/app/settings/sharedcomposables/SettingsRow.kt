// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.sharedcomposables

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.BuildConfig
import com.neeva.app.LocalChromiumVersion
import com.neeva.app.R
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.clearbrowsing.ClearDataButtonContainer
import com.neeva.app.settings.defaultbrowser.SetDefaultBrowserRow
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.profile.ProfileRowContainer
import com.neeva.app.settings.profile.SubscriptionRow
import com.neeva.app.settings.sharedcomposables.subcomponents.CheckBoxGroup
import com.neeva.app.settings.sharedcomposables.subcomponents.CheckBoxItem
import com.neeva.app.settings.sharedcomposables.subcomponents.SettingsButtonRow
import com.neeva.app.settings.sharedcomposables.subcomponents.SettingsLinkRow
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.NeevaSwitch
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.NavigationRow
import com.neeva.app.ui.widgets.RadioButtonGroup

data class SettingsRowDataValues(
    val primaryLabel: String,
    val secondaryLabel: String? = null
)

@Composable
private fun getSettingsRowDataValues(
    rowData: SettingsRowData
): SettingsRowDataValues {
    // if there was no primaryLabelId set,
    // screenshot tests and developers will see that their new SettingsRow has no label
    var primaryLabel = rowData.primaryLabelId?.let { stringResource(it) } ?: ""
    var secondaryLabel = rowData.getSecondaryLabel()
    if (rowData.primaryLabelId == R.string.settings_neeva_browser_version) {
        primaryLabel = stringResource(rowData.primaryLabelId, BuildConfig.VERSION_NAME)
        secondaryLabel = stringResource(
            R.string.settings_chromium_version,
            LocalChromiumVersion.current
        )
    }
    return SettingsRowDataValues(primaryLabel, secondaryLabel)
}

@Composable
fun SettingsRow(
    rowData: SettingsRowData,
    settingsController: SettingsController,
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null
) {
    val rowDataValues = getSettingsRowDataValues(rowData)

    when (rowData.type) {
        SettingsRowType.BUTTON -> {
            SettingsButtonRow(
                primaryLabel = rowDataValues.primaryLabel,
                secondaryLabel = rowDataValues.secondaryLabel,
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
        }

        SettingsRowType.LINK -> {
            if (rowData.url != null) {
                SettingsLinkRow(
                    label = rowDataValues.primaryLabel,
                    openUrl = { settingsController.openUrl(rowData.url, rowData.openUrlViaIntent) }
                )
            }
        }

        SettingsRowType.TEXT -> {
            Text(
                rowDataValues.primaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    vertical = Dimensions.PADDING_TINY,
                    horizontal = Dimensions.PADDING_LARGE
                )
            )
        }

        SettingsRowType.TOGGLE -> {
            assert(rowData.settingsToggle != null)
            if (rowData.settingsToggle != null) {
                val toggleState = settingsController.getToggleState(rowData.settingsToggle)
                NeevaSwitch(
                    primaryLabel = rowData.settingsToggle
                        .primaryLabelId?.let { stringResource(it) } ?: "",
                    secondaryLabel = rowData.settingsToggle
                        .secondaryLabelId?.let { stringResource(it) },
                    enabled = rowData.enabled,
                    isChecked = toggleState.value,
                    onCheckedChange = settingsController
                        .getTogglePreferenceSetter(rowData.settingsToggle)
                )
            }
        }

        SettingsRowType.NAVIGATION -> {
            if (onClick != null) {
                // TODO(kobec): discuss with Dan to figure out a better way to deal with special cases like Set Android Default Browser
                // https://github.com/neevaco/neeva-android/pull/376#discussion_r816329896
                if (rowData.primaryLabelId == R.string.settings_default_browser) {
                    SetDefaultBrowserRow(
                        settingsController.getSetDefaultAndroidBrowserManager(),
                        navigateToPane = onClick
                    )
                } else {
                    NavigationRow(
                        primaryLabel = rowDataValues.primaryLabel,
                        secondaryLabel = rowDataValues.secondaryLabel,
                        enabled = rowData.enabled,
                        onClick = onClick
                    )
                }
            }
        }

        SettingsRowType.PROFILE -> {
            ProfileRowContainer(
                isSignedOut = settingsController.isSignedOut(),
                showSSOProviderAsPrimaryLabel = rowData.showSSOProviderAsPrimaryLabel,
                userData = settingsController.getNeevaUserData(),
                onClick = onClick
            )
        }

        SettingsRowType.CLEAR_DATA_BUTTON -> {
            ClearDataButtonContainer(
                getToggleState = settingsController::getToggleState,
                rowData = rowData,
                onClearBrowsingData = settingsController::clearBrowsingData
            )
        }

        SettingsRowType.SUBSCRIPTION -> {
            if (rowData.url != null) {
                SubscriptionRow(
                    subscriptionType = settingsController.getNeevaUserData().subscriptionType,
                    openUrl = {
                        settingsController.openUrl(rowData.url, rowData.openUrlViaIntent)
                    }
                )
            }
        }

        SettingsRowType.COOKIE_CUTTER_BLOCKING_STRENGTH -> {
            RadioButtonGroup(
                CookieCutterModel.BlockingStrength.values().map {
                    stringResource(it.title)
                },
                settingsController.getCookieCutterStrength().ordinal,
                onSelect = { index ->
                    val blockingStrength = CookieCutterModel.BlockingStrength.values()[index]
                    settingsController.setCookieCutterStrength(blockingStrength)
                }
            )
        }

        SettingsRowType.COOKIE_CUTTER_NOTICE_SELECTION -> {
            RadioButtonGroup(
                CookieCutterModel.CookieNoticeSelection.values().map {
                    stringResource(it.title)
                },
                settingsController.getCookieNoticeSelection().ordinal,
                onSelect = { index ->
                    val selection = CookieCutterModel.CookieNoticeSelection.values()[index]
                    settingsController.setCookieNoticeSelection(selection)
                }
            )
        }

        SettingsRowType.COOKIE_PREFERENCE_SELECTION -> {
            CheckBoxGroup(
                checkBoxOptions = CookieCutterModel.CookieNoticeCookies.values().map {
                    CheckBoxItem(it.title, it.description)
                },
                selectedOptionsIndex = settingsController.getCookieNoticePreferences()
                    .map { it.ordinal }.toSet(),
                onCheckedChange = { index, checked ->
                    val newSet = settingsController.getCookieNoticePreferences().toMutableSet()
                    if (checked) {
                        newSet.add(CookieCutterModel.CookieNoticeCookies.values()[index])
                    } else {
                        newSet.remove(CookieCutterModel.CookieNoticeCookies.values()[index])
                    }
                    settingsController.setCookieNoticePreferences(newSet)
                }
            )
        }
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
                settingsToggle = SettingsToggle.TRACKING_PROTECTION
            ),
            settingsController = mockSettingsControllerImpl
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
                type = SettingsRowType.LINK,
                primaryLabelId = R.string.debug_long_string_primary,
                url = Uri.parse("")
            ),
            settingsController = mockSettingsControllerImpl
        )
    }
}
