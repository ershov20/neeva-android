package com.neeva.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.appnav.AppNav
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.BrowserScaffold
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LocalEnvironmentState(
    val appNavModel: AppNavModel,
    val settingsDataModel: SettingsDataModel,
    val historyManager: HistoryManager,
    val dispatchers: Dispatchers,
    val sharedPreferencesModel: SharedPreferencesModel,
    val neevaUserToken: NeevaUserToken
)
val LocalEnvironment = compositionLocalOf<LocalEnvironmentState> { error("No value set") }
val LocalBrowserWrapper = compositionLocalOf<BrowserWrapper> { error("No value set") }

@Composable
fun ActivityUI(
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    webLayerModel: WebLayerModel,
    apolloClient: ApolloClient
) {
    val coroutineScope = rememberCoroutineScope()
    val appNavModel = LocalEnvironment.current.appNavModel
    val dispatchers = LocalEnvironment.current.dispatchers

    BrowserScaffold(bottomControlOffset, topControlOffset, webLayerModel)

    // All the other screens in the app.
    AppNav(
        webLayerModel = webLayerModel,
        appNavModel = appNavModel,
        modifier = Modifier.fillMaxSize()
    ) { space ->
        coroutineScope.launch(dispatchers.io) {
            webLayerModel.currentBrowser.activeTabModel.modifySpace(space, apolloClient)
            appNavModel.showBrowser()
        }
    }
}
