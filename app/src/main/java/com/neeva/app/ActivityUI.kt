package com.neeva.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.neeva.app.ui.DialogState
import com.neeva.app.ui.PopupModel
import com.neeva.app.ui.widgets.overlay.OverlaySheetHost
import com.neeva.app.ui.widgets.overlay.OverlaySheetModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.zeroquery.RegularProfileZeroQueryViewModel
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
    val popupModel: PopupModel,
    val spaceStore: SpaceStore,
)

val LocalAppNavModel = compositionLocalOf<AppNavModel> { error("No value set") }
val LocalBrowserToolbarModel = compositionLocalOf<BrowserToolbarModel> { error("No value set") }
val LocalBrowserWrapper = compositionLocalOf<BrowserWrapper> { error("No value set") }
val LocalCardsPaneModel = compositionLocalOf<CardsPaneModel> { error("No value set") }
val LocalEnvironment = compositionLocalOf<LocalEnvironmentState> { error("No value set") }
val LocalIsDarkTheme = compositionLocalOf { false }
val LocalNavHostController = compositionLocalOf<NavHostController> { error("No value set") }
val LocalSettingsController = compositionLocalOf<SettingsController> { error("No value set") }

val LocalFeedbackViewModel = compositionLocalOf<FeedbackViewModel> { error("No value set") }
val LocalRegularProfileZeroQueryViewModel =
    compositionLocalOf<RegularProfileZeroQueryViewModel> { error("No value set") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityUI(
    toolbarConfiguration: StateFlow<ToolbarConfiguration>,
    webLayerModel: WebLayerModel
) {
    val appNavModel = LocalAppNavModel.current
    val overlaySheetModel = LocalEnvironment.current.overlaySheetModel
    val popupModel = LocalEnvironment.current.popupModel

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = popupModel.snackbarHostState)
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
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
        }
    }

    val dialogState: DialogState? by popupModel.dialogState.collectAsState()
    dialogState?.let {
        Dialog(onDismissRequest = popupModel::hideDialog) {
            Surface(
                tonalElevation = 3.dp,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                it.content()
            }
        }
    }
}
