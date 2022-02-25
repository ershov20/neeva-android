package com.neeva.app.settings.sharedComposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsRow
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.ui.theme.Dimensions
import java.util.Locale

@Composable
fun SettingsGroupView(
    settingsViewModel: SettingsViewModel,
    groupData: SettingsGroupData,
    onClearBrowsingData: ((Map<String, Boolean>) -> Unit)? = null,
    buttonOnClicks: Map<Int, (() -> Unit)?> = mapOf()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .wrapContentHeight(align = Alignment.Bottom)
    ) {
        Column {
            if (groupData.titleId != null) {
                Text(
                    text = stringResource(groupData.titleId).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(10.dp)
                )
            }
            SettingRowsView(settingsViewModel, groupData, onClearBrowsingData, buttonOnClicks)
        }
    }
}

@Composable
fun SettingRowsView(
    settingsViewModel: SettingsViewModel,
    groupData: SettingsGroupData,
    onClearBrowsingData: ((Map<String, Boolean>) -> Unit)?,
    buttonOnClicks: Map<Int, (() -> Unit)?>
) {
    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surface
        )
    ) {
        groupData.rows.forEach { rowData ->
            val rowModifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = Dimensions.PADDING_LARGE)
            val onClick = buttonOnClicks[rowData.titleId]
            SettingsRow(
                rowData = rowData,
                settingsViewModel = settingsViewModel,
                onClearBrowsingData = onClearBrowsingData,
                onClick = onClick,
                modifier = rowModifier
            )
        }
    }
}
