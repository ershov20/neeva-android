package com.neeva.app

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.card.CardsContainer
import com.neeva.app.firstrun.FirstRunContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.neeva_menu.NeevaMenuSheet
import com.neeva.app.settings.SettingsContainer
import com.neeva.app.spaces.AddToSpaceSheet
import com.neeva.app.storage.Space
import com.neeva.app.storage.SpaceStore
import kotlinx.coroutines.launch

class AppNavModel(
    private val spaceStore: SpaceStore
) : ViewModel() {
    var navController: NavController? = null

    private fun setContentState(state: AppNavState) {
        navController?.navigate(state.name)
        if (state == AppNavState.ADD_TO_SPACE) {
            viewModelScope.launch {
                spaceStore.refresh()
            }
        }
    }

    fun isCurrentState(state: AppNavState) = navController?.currentBackStackEntry?.id == state.name

    fun showBrowser() = setContentState(AppNavState.BROWSER)
    fun showCardGrid() = setContentState(AppNavState.CARD_GRID)
    fun showAddToSpace() = setContentState(AppNavState.ADD_TO_SPACE)
    fun showNeevaMenu() = setContentState(AppNavState.NEEVA_MENU)
    fun showSettings() = setContentState(AppNavState.SETTINGS)
    fun showFirstRun() = setContentState(AppNavState.FIRST_RUN)

    @Suppress("UNCHECKED_CAST")
    class AppNavModelFactory(
        private val spaceStore: SpaceStore
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return AppNavModel(spaceStore) as T
        }
    }
}

@Composable
fun AppNav(
    appNavModel: AppNavModel,
    webLayerModel: WebLayerModel,
    spaceModifier: Space.Companion.SpaceModifier
) {
    val navController = rememberNavController()
    appNavModel.navController = navController
    val onMenuItem = { id: NeevaMenuItemId ->
        when (id) {
            NeevaMenuItemId.HOME -> {
                webLayerModel.loadUrl(Uri.parse(NeevaConstants.appURL))
                navController.navigate(AppNavState.BROWSER.name)
            }

            NeevaMenuItemId.SPACES -> {
                webLayerModel.loadUrl(Uri.parse(NeevaConstants.appSpacesURL))
                navController.navigate(AppNavState.BROWSER.name)
            }

            NeevaMenuItemId.SETTINGS -> {
                navController.navigate(AppNavState.SETTINGS.name)
            }

            NeevaMenuItemId.HISTORY -> {
                navController.navigate(AppNavState.HISTORY.name)
            }

            else -> {
                // Unimplemented screens.
            }
        }
    }

    NavHost(navController = navController, startDestination = AppNavState.BROWSER.name) {
        composable(AppNavState.BROWSER.name) {
            Box {}
        }

        composable(AppNavState.ADD_TO_SPACE.name) {
            AddToSpaceSheet(
                navController = navController,
                activeTabModel = webLayerModel.currentBrowser.activeTabModel,
                spaceModifier = spaceModifier
            )
        }

        composable(AppNavState.NEEVA_MENU.name) {
            NeevaMenuSheet(navController = navController, onMenuItem = onMenuItem)
        }

        composable(AppNavState.SETTINGS.name) {
            SettingsContainer(navController = navController) {
                webLayerModel.loadUrl(it)
                navController.navigate(AppNavState.BROWSER.name)
            }
        }

        // TODO(dan.alcantara): Should we be using the regular profile's favicon cache here?
        //                      The history UI always shows the regular profile's history.
        composable(AppNavState.HISTORY.name) {
            HistoryContainer(
                navController = navController,
                faviconCache = webLayerModel.currentBrowser.faviconCache
            ) {
                webLayerModel.loadUrl(it)
                navController.navigate(AppNavState.BROWSER.name)
            }
        }

        composable(AppNavState.CARD_GRID.name) {
            CardsContainer(
                navController = navController,
                webLayerModel = webLayerModel
            )
        }

        composable(AppNavState.FIRST_RUN.name) {
            FirstRunContainer(navController = navController)
        }
    }
}

enum class AppNavState {
    BROWSER,
    SETTINGS,
    ADD_TO_SPACE,
    NEEVA_MENU,
    HISTORY,
    CARD_GRID,
    FIRST_RUN
}
