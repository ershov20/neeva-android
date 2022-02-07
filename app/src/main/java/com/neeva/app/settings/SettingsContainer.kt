package com.neeva.app.settings

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.neeva.app.AppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.settings.clearBrowsingSettings.ClearBrowsingSettingsPane
import com.neeva.app.settings.mainSettings.MainSettingsPane
import com.neeva.app.settings.profileSettings.ProfileSettingsPane

interface SettingsPaneListener {
    val onBackPressed: () -> Unit
    val getTogglePreferenceSetter: (String?) -> ((Boolean) -> Unit)?
    val getToggleState: (String?) -> MutableState<Boolean>?
    val openUrl: (Uri) -> Unit
    val showFirstRun: () -> Unit
    val showClearBrowsingSettings: () -> Unit
    val showProfileSettings: () -> Unit
    val onClearHistory: () -> Unit
    val isSignedIn: () -> Boolean
}

fun getSettingsPaneListener(
    appNavModel: AppNavModel,
    settingsModel: SettingsModel,
    historyManager: HistoryManager
): SettingsPaneListener {
    return object : SettingsPaneListener {
        override val onBackPressed = appNavModel::popBackStack
        override val getTogglePreferenceSetter = settingsModel::getTogglePreferenceSetter
        override val getToggleState = settingsModel::getToggleState
        override val openUrl = appNavModel::openUrl
        override val showFirstRun = appNavModel::showFirstRun
        override val showClearBrowsingSettings = appNavModel::showClearBrowsingSettings
        override val showProfileSettings = appNavModel::showProfileSettings
        override val onClearHistory = historyManager::clearAllHistory
        override val isSignedIn: () -> Boolean = settingsModel::isSignedIn
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainSettingsContainer() {
    val appNavModel = LocalEnvironment.current.appNavModel
    val settingsModel = LocalEnvironment.current.settingsModel
    val historyManager = LocalEnvironment.current.historyManager
    MainSettingsPane(getSettingsPaneListener(appNavModel, settingsModel, historyManager))
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileSettingsContainer(webLayerModel: WebLayerModel) {
    val appNavModel = LocalEnvironment.current.appNavModel
    val settingsModel = LocalEnvironment.current.settingsModel
    val activeTabModel = webLayerModel.currentBrowser.activeTabModel
    ProfileSettingsPane(
        onBackPressed = appNavModel::popBackStack,
        signUserOut = {
            settingsModel.signOut()
            appNavModel.popBackStack()
            webLayerModel.clearNeevaCookies()
            webLayerModel.onAuthTokenUpdated()
            activeTabModel.reload()
        }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ClearBrowsingSettingsContainer(webLayerModel: WebLayerModel) {
    val appNavModel = LocalEnvironment.current.appNavModel
    val settingsModel = LocalEnvironment.current.settingsModel
    val historyManager = LocalEnvironment.current.historyManager
    ClearBrowsingSettingsPane(
        getSettingsPaneListener(appNavModel, settingsModel, historyManager),
        webLayerModel::clearBrowsingData
    )
}
