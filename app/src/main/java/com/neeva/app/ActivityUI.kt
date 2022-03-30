package com.neeva.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.neeva.app.appnav.AppNav
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.feedback.FeedbackViewModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.ui.BrowserScaffold
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.flow.StateFlow

/** Classes that should be passed around the entire Composable hierarchy. */
data class LocalEnvironmentState(
    val dispatchers: Dispatchers,
    val domainProvider: DomainProvider,
    val historyManager: HistoryManager,
    val neevaUser: NeevaUser,
    val settingsDataModel: SettingsDataModel,
    val sharedPreferencesModel: SharedPreferencesModel,
    val snackbarModel: SnackbarModel,
    val spaceStore: SpaceStore,
    val apolloWrapper: ApolloWrapper
)

val LocalAppNavModel = compositionLocalOf<AppNavModel> { error("No value set") }
val LocalBrowserWrapper = compositionLocalOf<BrowserWrapper> { error("No value set") }
val LocalCardsPaneModel = compositionLocalOf<CardsPaneModel> { error("No value set") }
val LocalEnvironment = compositionLocalOf<LocalEnvironmentState> { error("No value set") }
val LocalFeedbackViewModel = compositionLocalOf<FeedbackViewModel> { error("No value set") }
val LocalSetDefaultAndroidBrowserManager = compositionLocalOf<SetDefaultAndroidBrowserManager> {
    error("No value set")
}

@Composable
fun ActivityUI(
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    webLayerModel: WebLayerModel,
    onSignOut: () -> Unit
) {
    val appNavModel = LocalAppNavModel.current
    val snackbarModel = LocalEnvironment.current.snackbarModel

    Box {
        BrowserScaffold(bottomControlOffset, topControlOffset, webLayerModel)

        // All the other screens in the app.
        AppNav(
            webLayerModel = webLayerModel,
            appNavModel = appNavModel,
            onSignOut = onSignOut,
            modifier = Modifier.fillMaxSize()
        ) { space ->
            appNavModel.showBrowser()
            webLayerModel.currentBrowser.modifySpace(space.id)
        }

        SnackbarHost(
            hostState = snackbarModel.snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
