// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.sharedcomposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.neeva.app.R
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.ui.FullScreenDialogTopBar

@Composable
fun SettingsPane(
    settingsController: SettingsController,
    paneData: SettingsPaneDataInterface
) {
    /** Surface used to block touch propagation behind the surface. */
    Surface {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            FullScreenDialogTopBar(
                title = getTopAppBarTitle(settingsController, paneData),
                onBackPressed = settingsController::onBackPressed
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1.0f)
                    .semantics { testTag = "SettingsPaneItems" }
            ) {
                paneData.data.forEach { groupData ->
                    item {
                        SettingsGroupView(settingsController, groupData)
                    }
                }
            }
        }
    }
}

@Composable
fun getTopAppBarTitle(
    settingsController: SettingsController,
    paneData: SettingsPaneDataInterface
): String {
    return when {
        paneData.topAppBarTitleResId != -1 -> stringResource(paneData.topAppBarTitleResId)
        paneData.shouldShowUserName -> settingsController.getNeevaUserData().displayName ?: ""
        else -> stringResource(R.string.settings)
    }
}
