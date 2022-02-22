package com.neeva.app.appnav

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.MainThread
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.sharing.ShareModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Triggers navigations to various screens in the app. */
class AppNavModel(
    val navController: NavHostController,
    private val webLayerModel: WebLayerModel,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers
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
            .flowOn(dispatchers.main)
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
            .flowOn(dispatchers.main)
            .launchIn(coroutineScope)
    }

    /** Shows a specific screen or context sheet. */
    private fun show(destination: AppNavDestination) {
        if (navController.currentDestination?.route == destination.route) return
        navController.navigate(destination.route) {
            launchSingleTop = true

            // If the destination has an explicit parent, pop the stack all the way up to there.
            // This isn't strictly necessary for most screens, but avoids confusion about the
            // correct way to navigate somewhere using the NavController.
            destination.parent?.let { popUpTo(it.route) }
        }
    }

    fun popBackStack() {
        navController.popBackStack()
    }

    @MainThread
    fun showBrowser() {
        navController.popBackStack(
            route = AppNavDestination.BROWSER.route,
            inclusive = false
        )

        if (webLayerModel.currentBrowser.userMustBeShownCardGrid()) {
            showCardGrid()
        }
    }

    fun openUrl(url: Uri) {
        webLayerModel.loadUrl(url)
        showBrowser()
    }

    fun showCardGrid() = show(AppNavDestination.CARD_GRID)
    fun showAddToSpace() = show(AppNavDestination.ADD_TO_SPACE)
    fun showSettings() = show(AppNavDestination.SETTINGS)
    fun showProfileSettings() = show(AppNavDestination.PROFILE_SETTINGS)
    fun showClearBrowsingSettings() = show(AppNavDestination.CLEAR_BROWSING_SETTINGS)
    fun showFirstRun() = show(AppNavDestination.FIRST_RUN)
    fun showHistory() = show(AppNavDestination.HISTORY)

    fun onMenuItem(id: NeevaMenuItemId, context: Context? = null) =
        when (id) {
            NeevaMenuItemId.HOME -> {
                webLayerModel.loadUrl(Uri.parse(NeevaConstants.appURL))
                showBrowser()
            }

            NeevaMenuItemId.SPACES -> {
                webLayerModel.loadUrl(Uri.parse(NeevaConstants.appSpacesURL))
                showBrowser()
            }

            NeevaMenuItemId.SETTINGS -> {
                showSettings()
            }

            NeevaMenuItemId.HISTORY -> {
                showHistory()
            }

            NeevaMenuItemId.FORWARD -> {
                webLayerModel.currentBrowser.activeTabModel.goForward()
            }

            NeevaMenuItemId.REFRESH -> {
                webLayerModel.currentBrowser.activeTabModel.reload()
            }

            NeevaMenuItemId.SHARE -> {
                context?.let {
                    ShareModel().shareURL(
                        webLayerModel.currentBrowser.activeTabModel.urlFlow.value,
                        webLayerModel.currentBrowser.activeTabModel.titleFlow.value,
                        it
                    )
                }
            }

            NeevaMenuItemId.SHOW_PAGE_INFO -> {
                webLayerModel.currentBrowser.showPageInfo()
            }

            NeevaMenuItemId.FIND_IN_PAGE -> {
                webLayerModel.currentBrowser.activeTabModel.showFindInPage()
            }

            NeevaMenuItemId.UPDATE -> {
                context?.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.neeva.app")
                    )
                )
            }

            else -> {
                // Unimplemented screens.
            }
        }
}
