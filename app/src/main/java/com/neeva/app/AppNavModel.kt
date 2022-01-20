package com.neeva.app

import android.content.Context
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Triggers navigations to various screens in the app. */
class AppNavModel(@ApplicationContext appContext: Context) {
    val navController = NavHostController(appContext).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
    }

    private val _currentDestination = MutableStateFlow(navController.currentDestination)
    val currentDestination: StateFlow<NavDestination?> = _currentDestination

    init {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            _currentDestination.value = destination
        }
    }

    private fun setContentState(destination: AppNavDestination) {
        navController.navigate(destination.name)
    }

    fun isCurrentState(destination: AppNavDestination): Boolean {
        return navController.currentBackStackEntry?.destination?.route == destination.name
    }

    fun showBrowser() = setContentState(AppNavDestination.BROWSER)
    fun showCardGrid() = setContentState(AppNavDestination.CARD_GRID)
    fun showAddToSpace() = setContentState(AppNavDestination.ADD_TO_SPACE)
    fun showNeevaMenu() = setContentState(AppNavDestination.NEEVA_MENU)
    fun showSettings() = setContentState(AppNavDestination.SETTINGS)
    fun showFirstRun() = setContentState(AppNavDestination.FIRST_RUN)
    fun showHistory() = setContentState(AppNavDestination.HISTORY)
}
