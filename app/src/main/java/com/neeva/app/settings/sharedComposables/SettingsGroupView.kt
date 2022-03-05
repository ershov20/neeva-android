package com.neeva.app.settings.sharedComposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsRow
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.ui.theme.Dimensions
import java.util.Locale

@Composable
fun SettingsGroupView(
    settingsViewModel: SettingsViewModel,
    groupData: SettingsGroupData,
    setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager? = null,
    onClearBrowsingData: ((Map<String, Boolean>) -> Unit)? = null,
    buttonOnClicks: Map<Int, (() -> Unit)?> = mapOf()
) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Bottom)
    ) {
        if (groupData.titleId != null) {
            Text(
                text = stringResource(groupData.titleId).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(Dimensions.PADDING_SMALL)
            )
        }
        SettingRowsView(
            settingsViewModel,
            groupData,
            setDefaultAndroidBrowserManager,
            onClearBrowsingData,
            buttonOnClicks
        )
    }
}

@Composable
fun SettingRowsView(
    settingsViewModel: SettingsViewModel,
    groupData: SettingsGroupData,
    setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager?,
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
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = Dimensions.PADDING_LARGE)
            val onClick = buttonOnClicks[rowData.titleId]
            SettingsRow(
                rowData = rowData,
                settingsViewModel = settingsViewModel,
                setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
                onClearBrowsingData = onClearBrowsingData,
                onClick = onClick,
                modifier = rowModifier
            )
        }
    }
}
