// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.sharedcomposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.ui.SectionHeader

@Composable
fun SettingsGroupView(
    settingsController: SettingsController,
    groupData: SettingsGroupData
) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Bottom)
    ) {
        if (settingsController.isAdvancedSettingsAllowed() || !groupData.isForDebugOnly) {
            SectionHeader(groupData.titleId)

            SettingRowsView(
                settingsController,
                groupData
            )
        }
    }
}

@Composable
fun SettingRowsView(
    settingsController: SettingsController,
    groupData: SettingsGroupData
) {
    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surface
        )
    ) {
        groupData.rows.forEach { rowData ->
            SettingsRow(
                rowData = rowData,
                settingsController = settingsController,
                onClick = settingsController.getOnClickMap()[rowData.primaryLabelId],
                onDoubleClick = settingsController.getOnDoubleClickMap()[rowData.primaryLabelId]
            )
        }
    }
}
