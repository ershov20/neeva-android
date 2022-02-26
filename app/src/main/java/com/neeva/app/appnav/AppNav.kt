package com.neeva.app.appnav

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.LocalEnvironment
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.card.CardsContainer
import com.neeva.app.feedback.FeedbackView
import com.neeva.app.feedback.FeedbackViewModelImpl
import com.neeva.app.firstrun.FirstRunContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.settings.ProfileSettingsContainer
import com.neeva.app.settings.SettingsViewModelImpl
import com.neeva.app.settings.clearBrowsing.ClearBrowsingPane
import com.neeva.app.settings.main.MainSettingsPane
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserPane
import com.neeva.app.spaces.AddToSpaceSheet
import com.neeva.app.spaces.SpaceModifier

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNav(
    webLayerModel: WebLayerModel,
    appNavModel: AppNavModel,
    modifier: Modifier,
    spaceModifier: SpaceModifier
) {
    val settingsDataModel = LocalEnvironment.current.settingsDataModel
    val neevaUser = LocalEnvironment.current.neevaUser
    val historyManager = LocalEnvironment.current.historyManager
    val coroutineScope = rememberCoroutineScope()

    val settingsViewModel = remember {
        SettingsViewModelImpl(
            appNavModel,
            settingsDataModel,
            historyManager,
            neevaUser
        )
    }

    val feedbackViewModel = remember {
        FeedbackViewModelImpl(
            appNavModel,
            user = neevaUser,
            coroutineScope = coroutineScope
        ) {
            appNavModel.showBrowser()
            webLayerModel.currentBrowser.createTabWithUri(
                Uri.parse(NeevaConstants.appHelpCenterURL),
                parentTabId = null,
                isViaIntent = false
            )
        }
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
            Box {}
        }

        composable(AppNavDestination.ADD_TO_SPACE.route) {
            AddToSpaceSheet(
                webLayerModel = webLayerModel,
                spaceModifier = spaceModifier
            )
        }

        composable(AppNavDestination.SETTINGS.route) {
            MainSettingsPane(settingsViewModel)
        }

        composable(AppNavDestination.PROFILE_SETTINGS.route) {
            ProfileSettingsContainer(
                webLayerModel = webLayerModel,
                neevaUser = neevaUser,
                settingsViewModel = settingsViewModel
            )
        }

        composable(AppNavDestination.CLEAR_BROWSING_SETTINGS.route) {
            ClearBrowsingPane(
                settingsViewModel = settingsViewModel,
                webLayerModel::clearBrowsingData
            )
        }

        composable(AppNavDestination.SET_DEFAULT_BROWSER_SETTINGS.route) {
            SetDefaultAndroidBrowserPane(
                settingsViewModel = settingsViewModel
            )
        }

        composable(AppNavDestination.HISTORY.route) {
            HistoryContainer(
                faviconCache = webLayerModel.getRegularProfileFaviconCache()
            ) {
                webLayerModel.loadUrl(it)
                appNavModel.showBrowser()
            }
        }

        composable(AppNavDestination.CARD_GRID.route) {
            CardsContainer(
                webLayerModel = webLayerModel
            )
        }

        composable(AppNavDestination.FIRST_RUN.route) {
            FirstRunContainer()
        }

        composable(AppNavDestination.FEEDBACK.route) {
            feedbackViewModel.createScreenshot(
                context = LocalContext.current,
                view = LocalView.current
            )

            FeedbackView(
                feedbackViewModel = feedbackViewModel,
                currentURL = webLayerModel.currentBrowser.activeTabModel.urlFlow
            )
        }
    }
}
