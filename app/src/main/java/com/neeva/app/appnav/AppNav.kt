package com.neeva.app.appnav

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.LocalEnvironment
import com.neeva.app.LocalSetDefaultAndroidBrowserManager
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.CardsPane
import com.neeva.app.feedback.FeedbackView
import com.neeva.app.firstrun.LocalFirstRunModel
import com.neeva.app.firstrun.SignInScreenContainer
import com.neeva.app.firstrun.signup.SignUpLandingContainer
import com.neeva.app.firstrun.signup.SignUpWithOtherContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.settings.SettingsControllerImpl
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

    val settingsControllerImpl = remember(
        appNavModel,
        settingsDataModel,
        neevaUser,
        webLayerModel,
        onSignOut,
        setDefaultAndroidBrowserManager
    ) {
        SettingsControllerImpl(
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
                settingsController = settingsControllerImpl
            )
        }

        composable(AppNavDestination.PROFILE_SETTINGS.route) {
            ProfileSettingsPane(
                settingsController = settingsControllerImpl
            )
        }

        composable(AppNavDestination.CLEAR_BROWSING_SETTINGS.route) {
            ClearBrowsingPane(
                settingsController = settingsControllerImpl
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

        composable(AppNavDestination.SIGN_UP_LANDING_PAGE.route) {
            val firstRunModel = LocalFirstRunModel.current
            SignUpLandingContainer(
                launchLoginIntent = firstRunModel.getLaunchLoginIntent(LocalContext.current),
                openInCustomTabs = firstRunModel.openInCustomTabs(LocalContext.current),
                onClose = firstRunModel.getOnCloseOnboarding(appNavModel::showBrowser),
                navigateToSignIn = appNavModel::showSignIn,
                showSignUpWithOther = appNavModel::showSignUpWithOther
            )
        }

        composable(AppNavDestination.SIGN_UP_OTHER.route) {
            val firstRunModel = LocalFirstRunModel.current
            SignUpWithOtherContainer(
                launchLoginIntent = firstRunModel.getLaunchLoginIntent(LocalContext.current),
                onClose = firstRunModel.getOnCloseOnboarding(appNavModel::showBrowser),
                navigateToSignIn = appNavModel::showSignIn
            )
        }

        composable(AppNavDestination.SIGN_IN.route) {
            val firstRunModel = LocalFirstRunModel.current
            SignInScreenContainer(
                launchLoginIntent = firstRunModel.getLaunchLoginIntent(LocalContext.current),
                onClose = firstRunModel.getOnCloseOnboarding(appNavModel::showBrowser),
                navigateToSignUp = appNavModel::showSignUpLanding
            )
        }

        composable(AppNavDestination.FEEDBACK.route) {
            FeedbackView(
                currentURLFlow = webLayerModel.currentBrowser.activeTabModel.urlFlow
            )
        }
    }
}
