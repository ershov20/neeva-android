package com.neeva.app.settings.sharedComposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.ui.FullScreenDialogTopBar

@Composable
fun SettingsPane(
    settingsViewModel: SettingsViewModel,
    paneData: SettingsPaneDataInterface
) {
    /** Surface used to block touch propagation behind the surface. */
    Surface {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            FullScreenDialogTopBar(
                title = getTopAppBarTitle(settingsViewModel, paneData),
                onBackPressed = settingsViewModel::onBackPressed
            )

            LazyColumn(
                modifier = Modifier.weight(1.0f)
            ) {
                paneData.data.forEach { groupData ->
                    item {
                        SettingsGroupView(settingsViewModel, groupData)
                    }
                }
            }
        }
    }
}

@Composable
fun getTopAppBarTitle(
    settingsViewModel: SettingsViewModel,
    paneData: SettingsPaneDataInterface
): String {
    return when {
        paneData.topAppBarTitleResId != -1 -> stringResource(paneData.topAppBarTitleResId)
        paneData.shouldShowUserName -> settingsViewModel.getNeevaUserData().displayName ?: ""
        else -> stringResource(R.string.settings)
    }
}
