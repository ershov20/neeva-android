package com.neeva.app.settings

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.neeva.app.LocalEnvironment

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsContainer(
    onOpenUrl: (Uri) -> Unit
) {
    val appNavModel = LocalEnvironment.current.appNavModel
    val settingsModel = LocalEnvironment.current.settingsModel

    SettingsPane(
        onShowBrowser = appNavModel::showBrowser,
        onOpenUrl = onOpenUrl,
        getTogglePreferenceSetter = settingsModel::getTogglePreferenceSetter,
        getToggleState = settingsModel::getToggleState
    )
}
