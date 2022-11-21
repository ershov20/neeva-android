// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.clearbrowsing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark

@Composable
fun ClearBrowsingPane(
    settingsController: SettingsController,
    neevaConstants: NeevaConstants
) {
    var showDialog by remember { mutableStateOf(false) }
    val successMessage = stringResource(R.string.settings_selected_data_cleared_success)
    if (showDialog) {
        ClearBrowsingDialog(
            confirmAction = { timeClearingOption ->
                settingsController.clearBrowsingData(
                    ClearBrowsingPaneData.timeClearingOptionToggles
                        .mapNotNull { it.settingsToggle }
                        .associateWith { settingsController.getToggleState(it).value },
                    timeClearingOption
                )
                showDialog = false

                // TODO(and): Should wait for success of clearBrowsingData before showing
                // the snackbar. Several functions deep, the work is done async in a coroutine.
                settingsController.showDataClearedMessage(successMessage)
            },
            dismissAction = { showDialog = false }
        )
    }
    SettingsPane(
        settingsController,
        ClearBrowsingPaneData(
            neevaConstants,
            onClearDataButtonTapped = { showDialog = true }
        )
    )
}

@PortraitPreviews
@Composable
fun ClearBrowsingSettings_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = false,
        addBorder = false
    ) {
        ClearBrowsingPane(
            mockSettingsControllerImpl,
            neevaConstants = LocalNeevaConstants.current
        )
    }
}

@PortraitPreviewsDark
@Composable
fun ClearBrowsingSettings_Dark_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = true,
        addBorder = false
    ) {
        ClearBrowsingPane(
            mockSettingsControllerImpl,
            neevaConstants = LocalNeevaConstants.current
        )
    }
}
