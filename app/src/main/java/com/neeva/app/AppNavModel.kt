package com.neeva.app

import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Triggers navigations to various screens in the app. */
class AppNavModel(
    val navController: NavHostController,
    private val webLayerModel: WebLayerModel,
    private val coroutineScope: CoroutineScope
) {
    private val _currentDestination = MutableStateFlow(navController.currentDestination)
    val currentDestination: StateFlow<NavDestination?> = _currentDestination

    /** Keeps track of whether the back button should do anything. */
    private var backEnablingJob: Job? = null

    init {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            _currentDestination.value = destination
        }

        webLayerModel.browserWrapperFlow
            .onEach { updateBackEnablingJob(it) }
            .launchIn(coroutineScope)
    }

    /**
     * Replaces the [Job] that is currently keeping track of whether or not the [navController]
     * should do anything when they hit back.
     *
     * Currently, this just prevents the user from backing out of the [CardGrid] destination when
     * the current [browserWrapper] has no tabs open.
     */
    private fun updateBackEnablingJob(browserWrapper: BrowserWrapper) {
        backEnablingJob?.cancel()
        backEnablingJob = browserWrapper.userMustStayInCardGridFlow
            .combine(currentDestination) { mustStay, currentDestination ->
                mustStay && currentDestination?.route == AppNavDestination.CARD_GRID.route
            }
            .onEach { userMustStay -> navController.enableOnBackPressed(!userMustStay) }
            .launchIn(coroutineScope)
    }

    /** Shows a screen and allows the user to return to the browser after closing it. */
    private fun showSecondaryScreen(destination: AppNavDestination) {
        navController.navigate(destination.route) {
            launchSingleTop = true
            popUpTo(AppNavDestination.BROWSER.route)
        }
    }

    fun showBrowser() {
        navController.popBackStack(
            route = AppNavDestination.BROWSER.route,
            inclusive = false
        )

        if (webLayerModel.currentBrowser.userMustBeShownCardGrid()) {
            showCardGrid()
        }
    }

    fun showCardGrid() = showSecondaryScreen(AppNavDestination.CARD_GRID)
    fun showAddToSpace() = showSecondaryScreen(AppNavDestination.ADD_TO_SPACE)
    fun showNeevaMenu() = showSecondaryScreen(AppNavDestination.NEEVA_MENU)
    fun showSettings() = showSecondaryScreen(AppNavDestination.SETTINGS)
    fun showFirstRun() = showSecondaryScreen(AppNavDestination.FIRST_RUN)
    fun showHistory() = showSecondaryScreen(AppNavDestination.HISTORY)
}
