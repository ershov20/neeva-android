package com.neeva.app.settings

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.neeva.app.AppNavState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsContainer(navController: NavController, onOpenUrl: (Uri) -> Unit) {
    SettingsPane(
        onShowBrowser = { navController.navigate(AppNavState.BROWSER.name) },
        onOpenUrl = onOpenUrl
    )
}
