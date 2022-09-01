// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.defaultbrowser

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.settings.sharedcomposables.subcomponents.SettingsLabelRow
import com.neeva.app.ui.widgets.NavigationRow

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
            secondaryLabel = stringResource(id = R.string.neeva)
        )
    } else {
        NavigationRow(
            primaryLabel = stringResource(id = R.string.switch_default_browser_title),
            onClick = {
                if (setDefaultAndroidBrowserManager.isRoleManagerAvailable()) {
                    setDefaultAndroidBrowserManager.requestToBeDefaultBrowser {
                        // Do nothing when the dialog closes because we're staying on Settings.
                    }
                } else {
                    navigateToPane()
                }
            }
        )
    }
}
