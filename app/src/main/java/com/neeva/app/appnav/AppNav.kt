package com.neeva.app.appnav

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.LocalEnvironment
import com.neeva.app.LocalSettingsControllerImpl
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.CardsPane
import com.neeva.app.feedback.FeedbackView
import com.neeva.app.firstrun.signInFlowNavGraph
import com.neeva.app.history.HistoryContainer
import com.neeva.app.settings.clearBrowsing.ClearBrowsingPane
import com.neeva.app.settings.featureFlags.FeatureFlagsPane
import com.neeva.app.settings.main.MainSettingsPane
import com.neeva.app.settings.profile.ProfileSettingsPane
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserPane
import com.neeva.app.ui.BrowserScaffold
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNav(
    toolbarConfiguration: StateFlow<ToolbarConfiguration>,
    webLayerModel: WebLayerModel,
    appNavModel: AppNavModel,
    modifier: Modifier
) {
    val settingsControllerImpl = LocalSettingsControllerImpl.current
    val neevaConstants = LocalEnvironment.current.neevaConstants

    AnimatedNavHost(
        navController = appNavModel.navController,
        startDestination = AppNavDestination.BROWSER.route,
        enterTransition = ::enterTransitionFactory,
        exitTransition = ::exitTransitionFactory,
        popEnterTransition = ::popEnterTransitionFactory,
        popExitTransition = ::popExitTransitionFactory,
        modifier = modifier
    ) {
        composable(AppNavDestination.BROWSER.route) {
            BrowserScaffold(toolbarConfiguration, webLayerModel)
        }

        composable(AppNavDestination.SETTINGS.route) {
            MainSettingsPane(
                settingsController = settingsControllerImpl,
                neevaConstants = neevaConstants
            )
        }

        composable(AppNavDestination.PROFILE_SETTINGS.route) {
            ProfileSettingsPane(
                settingsController = settingsControllerImpl,
                neevaConstants = neevaConstants
            )
        }

        composable(AppNavDestination.CLEAR_BROWSING_SETTINGS.route) {
            ClearBrowsingPane(
                settingsController = settingsControllerImpl,
                neevaConstants = neevaConstants
            )
        }

        composable(AppNavDestination.SET_DEFAULT_BROWSER_SETTINGS.route) {
            SetDefaultAndroidBrowserPane(
                settingsController = settingsControllerImpl
            )
        }

        composable(AppNavDestination.LOCAL_FEATURE_FLAGS_SETTINGS.route) {
            FeatureFlagsPane(
                settingsController = settingsControllerImpl
            )
        }

        composable(AppNavDestination.HISTORY.route) {
            HistoryContainer(
                faviconCache = webLayerModel.getRegularProfileFaviconCache()
            ) {
                appNavModel.openUrl(it)
            }
        }

        composable(AppNavDestination.CARD_GRID.route) {
            CardsPane(
                webLayerModel = webLayerModel
            )
        }

        composable(AppNavDestination.FEEDBACK.route) {
            FeedbackView(
                currentURLFlow = webLayerModel.currentBrowser.activeTabModel.urlFlow
            )
        }

        signInFlowNavGraph()
    }
}
