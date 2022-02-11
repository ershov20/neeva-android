package com.neeva.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.neeva.app.appnav.AppNav
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.NeevaUser
import com.neeva.app.ui.BrowserScaffold
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LocalEnvironmentState(
    val dispatchers: Dispatchers,
    val domainProvider: DomainProvider,
    val historyManager: HistoryManager,
    val neevaUser: NeevaUser,
    val settingsDataModel: SettingsDataModel,
    val sharedPreferencesModel: SharedPreferencesModel,
    val spaceStore: SpaceStore
)
val LocalEnvironment = compositionLocalOf<LocalEnvironmentState> { error("No value set") }
val LocalBrowserWrapper = compositionLocalOf<BrowserWrapper> { error("No value set") }
val LocalAppNavModel = compositionLocalOf<AppNavModel> { error("No value set") }

@Composable
fun ActivityUI(
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    webLayerModel: WebLayerModel
) {
    val coroutineScope = rememberCoroutineScope()
    val appNavModel = LocalAppNavModel.current
    val dispatchers = LocalEnvironment.current.dispatchers

    BrowserScaffold(bottomControlOffset, topControlOffset, webLayerModel)

    // All the other screens in the app.
    AppNav(
        webLayerModel = webLayerModel,
        appNavModel = appNavModel,
        modifier = Modifier.fillMaxSize()
    ) { space ->
        appNavModel.showBrowser()

        coroutineScope.launch {
            withContext(dispatchers.io) {
                webLayerModel.currentBrowser.activeTabModel.modifySpace(space.id)
            }
        }
    }
}
