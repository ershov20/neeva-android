package com.neeva.app.settings

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.neeva.app.AppNavState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsContainer(
    navController: NavController,
    settingsModel: SettingsModel,
    onOpenUrl: (Uri) -> Unit
) {
    SettingsPane(
        onShowBrowser = { navController.navigate(AppNavState.BROWSER.name) },
        onOpenUrl = onOpenUrl,
        getTogglePreferenceSetter = settingsModel::getTogglePreferenceSetter,
        getToggleState = settingsModel::getToggleState
    )
}
