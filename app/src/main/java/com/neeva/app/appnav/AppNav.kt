package com.neeva.app.appnav

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.LocalEnvironment
import com.neeva.app.LocalSetDefaultAndroidBrowserManager
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.card.CardsContainer
import com.neeva.app.feedback.FeedbackPreviewImageView
import com.neeva.app.feedback.FeedbackView
import com.neeva.app.feedback.FeedbackViewModelImpl
import com.neeva.app.firstrun.FirstRunContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.settings.SettingsViewModelImpl
import com.neeva.app.settings.clearBrowsing.ClearBrowsingPane
import com.neeva.app.settings.featureFlags.FeatureFlagsPane
import com.neeva.app.settings.main.MainSettingsPane
import com.neeva.app.settings.profile.ProfileSettingsPane
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
    val setDefaultAndroidBrowserManager = LocalSetDefaultAndroidBrowserManager.current
    val coroutineScope = rememberCoroutineScope()

    val settingsViewModel = remember(
        appNavModel,
        settingsDataModel,
        neevaUser,
        webLayerModel,
        setDefaultAndroidBrowserManager
    ) {
        SettingsViewModelImpl(
            appNavModel,
            settingsDataModel,
            neevaUser,
            webLayerModel,
            setDefaultAndroidBrowserManager
        )
    }

    val feedbackViewModel = remember(appNavModel, neevaUser, coroutineScope, webLayerModel) {
        FeedbackViewModelImpl(
            appNavModel,
            user = neevaUser,
            coroutineScope = coroutineScope
        ) {
            appNavModel.openUrl(Uri.parse(NeevaConstants.appHelpCenterURL))
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
            CardsContainer(
                webLayerModel = webLayerModel
            )
        }

        composable(AppNavDestination.FIRST_RUN.route) {
            FirstRunContainer()
        }

        composable(AppNavDestination.FEEDBACK.route) {
            FeedbackView(
                feedbackViewModel = feedbackViewModel,
                currentURL = webLayerModel.currentBrowser.activeTabModel.urlFlow
            )
        }

        composable(AppNavDestination.FEEDBACK_PREVIEW_IMAGE.route) {
            FeedbackPreviewImageView(
                appNavModel = appNavModel,
                imageUri = feedbackViewModel.imageUri
            )
        }
    }
}
