package com.neeva.app.settings.sharedComposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsRow
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.ui.SectionHeader

@Composable
fun SettingsGroupView(
    settingsViewModel: SettingsViewModel,
    groupData: SettingsGroupData
) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Bottom)
    ) {
        if (settingsViewModel.isDebugMode() || !groupData.isForDebugOnly) {
            groupData.titleId?.let { SectionHeader(it) }

            SettingRowsView(
                settingsViewModel,
                groupData
            )
        }
    }
}

@Composable
fun SettingRowsView(
    settingsViewModel: SettingsViewModel,
    groupData: SettingsGroupData
) {
    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surface
        )
    ) {
        groupData.rows.forEach { rowData ->
            var onClick = settingsViewModel.getMainSettingsNavigation()[rowData.titleId]
            SettingsRow(
                rowData = rowData,
                settingsViewModel = settingsViewModel,
                onClick = onClick,
                modifier = Modifier
                    .then(
                        if (groupData.isForDebugOnly) {
                            Modifier.background(MaterialTheme.colorScheme.errorContainer)
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.surface)
                        }
                    )
                    .then(SettingsUIConstants.rowModifier)
            )
        }
    }
}
