package com.neeva.app.settings.sharedComposables

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
            groupData.titleId?.let { SectionHeader(it) }

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
                onClick = settingsController.getOnClickMap(false)[rowData.primaryLabelId],
                onDoubleClick = settingsController.getOnDoubleClickMap()[rowData.primaryLabelId]
            )
        }
    }
}
