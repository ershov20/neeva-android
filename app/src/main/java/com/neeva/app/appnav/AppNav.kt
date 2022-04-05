package com.neeva.app.appnav

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.LocalEnvironment
import com.neeva.app.LocalSetDefaultAndroidBrowserManager
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.CardsPane
import com.neeva.app.feedback.FeedbackView
import com.neeva.app.firstrun.FirstRunContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.settings.SettingsViewModelImpl
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
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    webLayerModel: WebLayerModel,
    appNavModel: AppNavModel,
    onSignOut: () -> Unit,
    modifier: Modifier
) {
    val settingsDataModel = LocalEnvironment.current.settingsDataModel
    val neevaUser = LocalEnvironment.current.neevaUser
    val setDefaultAndroidBrowserManager = LocalSetDefaultAndroidBrowserManager.current

    val settingsViewModel = remember(
        appNavModel,
        settingsDataModel,
        neevaUser,
        webLayerModel,
        onSignOut,
        setDefaultAndroidBrowserManager
    ) {
        SettingsViewModelImpl(
            appNavModel,
            settingsDataModel,
            neevaUser,
            webLayerModel,
            onSignOut,
            setDefaultAndroidBrowserManager
        )
    }

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
            BrowserScaffold(bottomControlOffset, topControlOffset, webLayerModel)
        }

        composable(AppNavDestination.SETTINGS.route) {
            MainSettingsPane(
                settingsViewModel = settingsViewModel
            )
        }

        composable(AppNavDestination.PROFILE_SETTINGS.route) {
            ProfileSettingsPane(
                settingsViewModel = settingsViewModel
            )
        }

        composable(AppNavDestination.CLEAR_BROWSING_SETTINGS.route) {
            ClearBrowsingPane(
                settingsViewModel = settingsViewModel
            )
        }

        composable(AppNavDestination.SET_DEFAULT_BROWSER_SETTINGS.route) {
            SetDefaultAndroidBrowserPane(
                settingsViewModel = settingsViewModel
            )
        }

        composable(AppNavDestination.LOCAL_FEATURE_FLAGS_SETTINGS.route) {
            FeatureFlagsPane(
                settingsViewModel = settingsViewModel
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

        composable(AppNavDestination.FIRST_RUN.route) {
            FirstRunContainer()
        }

        composable(AppNavDestination.FEEDBACK.route) {
            FeedbackView(
                currentURLFlow = webLayerModel.currentBrowser.activeTabModel.urlFlow
            )
        }
    }
}
