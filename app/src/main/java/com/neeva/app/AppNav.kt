package com.neeva.app

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.card.CardsContainer
import com.neeva.app.firstrun.FirstRunContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.neeva_menu.NeevaMenuSheet
import com.neeva.app.settings.ProfileSettingsContainer
import com.neeva.app.settings.SettingsViewModelImpl
import com.neeva.app.settings.clearBrowsing.ClearBrowsingPane
import com.neeva.app.settings.main.MainSettingsPane
import com.neeva.app.spaces.AddToSpaceSheet
import com.neeva.app.spaces.Space

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNav(
    webLayerModel: WebLayerModel,
    appNavModel: AppNavModel,
    modifier: Modifier,
    spaceModifier: Space.Companion.SpaceModifier
) {
    val settingsViewModel = SettingsViewModelImpl(
        appNavModel,
        LocalEnvironment.current.settingsDataModel,
        LocalEnvironment.current.historyManager
    )

    AnimatedNavHost(
        navController = appNavModel.navController,
        startDestination = AppNavDestination.BROWSER.route,
        modifier = modifier
    ) {
        composable(AppNavDestination.BROWSER.route) {
            Box {}
        }

        composable(
            AppNavDestination.ADD_TO_SPACE.route,
            enterTransition = enterSlideTransition(
                initialRoute = AppNavDestination.BROWSER.route,
                AnimatedContentScope.SlideDirection.Up
            ),
            exitTransition = exitSlideTransition(
                targetRoute = AppNavDestination.BROWSER.route,
                AnimatedContentScope.SlideDirection.Down
            )
        ) {
            AddToSpaceSheet(
                webLayerModel = webLayerModel,
                spaceModifier = spaceModifier
            )
        }

        composable(
            AppNavDestination.NEEVA_MENU.route,
            enterTransition = enterSlideTransition(
                initialRoute = AppNavDestination.BROWSER.route,
                AnimatedContentScope.SlideDirection.Up
            ),
            exitTransition = exitSlideTransition(
                targetRoute = AppNavDestination.BROWSER.route,
                AnimatedContentScope.SlideDirection.Down
            )
        ) {
            NeevaMenuSheet(onMenuItem = { appNavModel.onMenuItem(it) })
        }

        composable(
            AppNavDestination.SETTINGS.route,
            enterTransition = enterSlideTransition(
                initialRoute = AppNavDestination.NEEVA_MENU.route,
                AnimatedContentScope.SlideDirection.Start
            ),
            exitTransition = exitSlideTransition(
                targetRoute = AppNavDestination.BROWSER.route,
                AnimatedContentScope.SlideDirection.End
            )
        ) {
            MainSettingsPane(settingsViewModel)
        }

        composable(
            AppNavDestination.PROFILE_SETTINGS.route,
            enterTransition = enterSlideTransition(
                initialRoute = AppNavDestination.SETTINGS.route,
                AnimatedContentScope.SlideDirection.Start
            ),
            exitTransition = exitSlideTransition(
                targetRoute = AppNavDestination.BROWSER.route,
                AnimatedContentScope.SlideDirection.End
            )
        ) {
            ProfileSettingsContainer(webLayerModel)
        }

        composable(
            AppNavDestination.CLEAR_BROWSING_SETTINGS.route,
            enterTransition = enterSlideTransition(
                initialRoute = AppNavDestination.SETTINGS.route,
                AnimatedContentScope.SlideDirection.Start
            ),
            exitTransition = exitSlideTransition(
                targetRoute = AppNavDestination.SETTINGS.route,
                AnimatedContentScope.SlideDirection.End
            )
        ) {
            ClearBrowsingPane(
                settingsViewModel = settingsViewModel,
                webLayerModel::clearBrowsingData
            )
        }

        composable(
            AppNavDestination.HISTORY.route,
            enterTransition = enterSlideTransition(
                initialRoute = AppNavDestination.NEEVA_MENU.route,
                AnimatedContentScope.SlideDirection.Start
            ),
            exitTransition = exitSlideTransition(
                targetRoute = AppNavDestination.BROWSER.route,
                AnimatedContentScope.SlideDirection.End
            )
        ) {
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
    }
}
