package com.neeva.app.settings.sharedComposables

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.BuildConfig
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.clearBrowsing.ClearDataButtonView
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.profile.ProfileRowContainer
import com.neeva.app.settings.profile.SubscriptionRow
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultBrowserRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsButtonRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsLinkRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsNavigationRow
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.NeevaSwitch

data class SettingsRowDataValues(
    val primaryLabel: String,
    val secondaryLabel: String? = null
)

@Composable
private fun getSettingsRowDataValues(
    rowData: SettingsRowData
): SettingsRowDataValues {
    var primaryLabel = stringResource(rowData.primaryLabelId)
    val secondaryLabel = rowData.secondaryLabelId?.let { stringResource(it) }
    val versionString = BuildConfig.VERSION_NAME
    if (rowData.primaryLabelId == R.string.settings_neeva_browser_version) {
        primaryLabel = stringResource(rowData.primaryLabelId, versionString)
    }
    return SettingsRowDataValues(primaryLabel, secondaryLabel)
}

@Composable
fun SettingsRow(
    rowData: SettingsRowData,
    isForDebugOnly: Boolean = false,
    settingsController: SettingsController,
    onClick: (() -> Unit)? = null
) {
    val rowDataValues = getSettingsRowDataValues(rowData)

    when (rowData.type) {
        SettingsRowType.BUTTON -> {
            onClick?.let {
                SettingsButtonRow(
                    label = rowDataValues.primaryLabel,
                    onClick = it
                )
            }
        }

        SettingsRowType.LINK -> {
            if (rowData.url != null) {
                SettingsLinkRow(
                    label = rowDataValues.primaryLabel,
                    openUrl = { settingsController.openUrl(rowData.url, rowData.openUrlViaIntent) }
                )
            }
        }

        SettingsRowType.TOGGLE -> {
            val toggleState = settingsController.getToggleState(rowData.togglePreferenceKey)
            if (toggleState != null && rowData.togglePreferenceKey != null) {
                NeevaSwitch(
                    primaryLabel = rowDataValues.primaryLabel,
                    secondaryLabel = rowDataValues.secondaryLabel,
                    enabled = rowData.enabled,
                    isChecked = toggleState.value,
                    onCheckedChange = { newToggleValue ->
                        settingsController.getTogglePreferenceSetter(rowData.togglePreferenceKey)
                            ?.invoke(newToggleValue)
                    }
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
                    SettingsNavigationRow(
                        primaryLabel = rowDataValues.primaryLabel,
                        enabled = rowData.enabled,
                        onClick = onClick,
                        isForDebugOnly = isForDebugOnly
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
            ClearDataButtonView(
                getToggleState = settingsController::getToggleState,
                rowData = rowData,
                onClearBrowsingData = settingsController::clearBrowsingData
            )
        }

        SettingsRowType.SUBSCRIPTION -> {
            val appMembershipURL = Uri.parse(NeevaConstants.appMembershipURL)
            SubscriptionRow(
                subscriptionType = settingsController.getNeevaUserData().subscriptionType,
                openUrl = { settingsController.openUrl(appMembershipURL, rowData.openUrlViaIntent) }
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
                R.string.debug_long_string_primary,
                togglePreferenceKey = "toggle preference key"
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
                url = Uri.parse(""),
                togglePreferenceKey = ""
            ),
            settingsController = mockSettingsControllerImpl
        )
    }
}
