package com.neeva.app.settings.setDefaultAndroidBrowser

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsLabelRow
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsNavigationRow

@Composable
fun SetDefaultBrowserRow(
    setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
    navigateToPane: () -> Unit
) {
    /**
     * Since there is no callback (for Android devices with system images pre-Q) to check if the
     * default browser has been updated, the only way to make sure that Neeva Settings reflected
     * accurate info about Android Settings was to check to see if it is the defaultBrowser every
     * time the user opens Neeva Settings.
     */
    setDefaultAndroidBrowserManager.updateIsDefaultBrowser()

    if (setDefaultAndroidBrowserManager.isDefaultBrowser.value) {
        SettingsLabelRow(
            primaryLabel = stringResource(id = R.string.settings_default_browser),
            secondaryLabel = stringResource(id = R.string.company_name)
        )
    } else if (setDefaultAndroidBrowserManager.isRoleManagerAvailable()) {
        SettingsNavigationRow(
            primaryLabel = stringResource(id = R.string.set_neeva_as_default_browser),
            onClick = { setDefaultAndroidBrowserManager.requestToBeDefaultBrowser() }
        )
    } else {
        SettingsNavigationRow(
            primaryLabel = stringResource(id = R.string.set_neeva_as_default_browser),
            onClick = navigateToPane
        )
    }
}
