package com.neeva.app

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.card.CardsContainer
import com.neeva.app.firstrun.FirstRun
import com.neeva.app.firstrun.FirstRunContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.neeva_menu.NeevaMenuSheet
import com.neeva.app.settings.SettingsContainer
import com.neeva.app.settings.SettingsModel
import com.neeva.app.spaces.AddToSpaceSheet
import com.neeva.app.spaces.Space

@Composable
fun AppNav(
    webLayerModel: WebLayerModel,
    settingsModel: SettingsModel,
    modifier: Modifier,
    spaceModifier: Space.Companion.SpaceModifier
) {
    val browserWrapper = LocalEnvironment.current.browserWrapper
    val appNavModel = LocalEnvironment.current.appNavModel

    val onMenuItem = { id: NeevaMenuItemId ->
        when (id) {
            NeevaMenuItemId.HOME -> {
                webLayerModel.loadUrl(Uri.parse(NeevaConstants.appURL))
                appNavModel.showBrowser()
            }

            NeevaMenuItemId.SPACES -> {
                webLayerModel.loadUrl(Uri.parse(NeevaConstants.appSpacesURL))
                appNavModel.showBrowser()
            }

            NeevaMenuItemId.SETTINGS -> {
                appNavModel.showSettings()
            }

            NeevaMenuItemId.HISTORY -> {
                appNavModel.showHistory()
            }

            else -> {
                // Unimplemented screens.
            }
        }
    }

    NavHost(
        navController = appNavModel.navController,
        startDestination = AppNavDestination.BROWSER.route,
        modifier = modifier
    ) {
        composable(AppNavDestination.BROWSER.route) {
            Box {}
        }

        composable(AppNavDestination.ADD_TO_SPACE.route) {
            AddToSpaceSheet(
                activeTabModel = browserWrapper.activeTabModel,
                spaceModifier = spaceModifier
            )
        }
        composable(AppNavDestination.NEEVA_MENU.route) {
            NeevaMenuSheet(onMenuItem = onMenuItem)
        }

        composable(AppNavDestination.SETTINGS.route) {
            SettingsContainer(settingsModel) {
                webLayerModel.loadUrl(it)
                appNavModel.showBrowser()
            }
        }

        // TODO(dan.alcantara): Should we be using the regular profile's favicon cache here?
        //                      The history UI always shows the regular profile's history.
        composable(AppNavDestination.HISTORY.route) {
            HistoryContainer(
                faviconCache = browserWrapper.faviconCache
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

    // TODO(dan.alcantara): Not the best place to do this, but because of the way AppNav and
    //                      AppNavModel are currently intertwined, it's the best option we've
    //                      got until that's fixed.
    val context = LocalContext.current
    LaunchedEffect(true) {
        if (FirstRun.shouldShowFirstRun(context)) {
            appNavModel.showFirstRun()
            FirstRun.firstRunDone(context)
        }
    }
}
