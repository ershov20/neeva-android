package com.neeva.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.neeva.app.appnav.AppNav
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.toolbar.BrowserToolbarModel
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.feedback.FeedbackViewModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.ui.widgets.overlay.OverlaySheetHost
import com.neeva.app.ui.widgets.overlay.OverlaySheetModel
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.flow.StateFlow

/** Classes that should be passed around the entire Composable hierarchy. */
data class LocalEnvironmentState(
    val apolloWrapper: ApolloWrapper,
    val clientLogger: ClientLogger,
    val dispatchers: Dispatchers,
    val domainProvider: DomainProvider,
    val historyManager: HistoryManager,
    val neevaConstants: NeevaConstants,
    val neevaUser: NeevaUser,
    val overlaySheetModel: OverlaySheetModel,
    val settingsDataModel: SettingsDataModel,
    val sharedPreferencesModel: SharedPreferencesModel,
    val snackbarModel: SnackbarModel,
    val spaceStore: SpaceStore,
)

val LocalAppNavModel = compositionLocalOf<AppNavModel> { error("No value set") }
val LocalBrowserToolbarModel = compositionLocalOf<BrowserToolbarModel> { error("No value set") }
val LocalBrowserWrapper = compositionLocalOf<BrowserWrapper> { error("No value set") }
val LocalCardsPaneModel = compositionLocalOf<CardsPaneModel> { error("No value set") }
val LocalEnvironment = compositionLocalOf<LocalEnvironmentState> { error("No value set") }
val LocalFeedbackViewModel = compositionLocalOf<FeedbackViewModel> { error("No value set") }
val LocalIsDarkTheme = compositionLocalOf { false }
val LocalNavHostController = compositionLocalOf<NavHostController> { error("No value set") }
val LocalSettingsController = compositionLocalOf<SettingsController> { error("No value set") }

@Composable
fun ActivityUI(
    toolbarConfiguration: StateFlow<ToolbarConfiguration>,
    webLayerModel: WebLayerModel
) {
    val appNavModel = LocalAppNavModel.current
    val overlaySheetModel = LocalEnvironment.current.overlaySheetModel
    val snackbarModel = LocalEnvironment.current.snackbarModel

    Box {
        AppNav(
            toolbarConfiguration = toolbarConfiguration,
            webLayerModel = webLayerModel,
            appNavModel = appNavModel,
            modifier = Modifier.fillMaxSize()
        )

        OverlaySheetHost(
            hostState = overlaySheetModel.hostState,
            onDismiss = overlaySheetModel::hideOverlaySheet
        )

        SnackbarHost(
            hostState = snackbarModel.snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
