package com.neeva.app.appnav

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.annotation.MainThread
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.ui.SnackbarModel
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
    private val context: Context,
    val navController: NavHostController,
    private val webLayerModel: WebLayerModel,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val snackbarModel: SnackbarModel,
    private val clientLogger: ClientLogger
) {
    private val _currentDestination = MutableStateFlow(navController.currentDestination)
    val currentDestination: StateFlow<NavDestination?> = _currentDestination

    /** Keeps track of whether the back button should do anything. */
    private var backEnablingJob: Job? = null

    init {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            _currentDestination.value = destination
        }

        webLayerModel.currentBrowserFlow
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
        webLayerModel.currentBrowser.loadUrl(
            uri = url,
            inNewTab = true,
            onLoadStarted = this::showBrowser
        )
    }

    fun openAndroidDefaultBrowserSettings() {
        safeStartActivityForIntent(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }

    fun openUrlViaIntent(uri: Uri) {
        safeStartActivityForIntent(Intent(Intent.ACTION_VIEW, uri))
        showBrowser()
    }

    /** Safely fire an Intent out. */
    fun safeStartActivityForIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            snackbarModel.show(context.getString(R.string.generic_error))
            Log.e(TAG, "Failed to start Activity for $intent")
        }
    }

    fun showCardGrid() = show(AppNavDestination.CARD_GRID)
    fun showAddToSpace() = show(AppNavDestination.ADD_TO_SPACE)
    fun showSettings() = show(AppNavDestination.SETTINGS)
    fun showProfileSettings() = show(AppNavDestination.PROFILE_SETTINGS)
    fun showClearBrowsingSettings() = show(AppNavDestination.CLEAR_BROWSING_SETTINGS)
    fun showDefaultBrowserSettings() = show(AppNavDestination.SET_DEFAULT_BROWSER_SETTINGS)
    fun showLocalFeatureFlagsPane() = show(AppNavDestination.LOCAL_FEATURE_FLAGS_SETTINGS)
    fun showFirstRun() {
        show(AppNavDestination.FIRST_RUN)
        clientLogger.logCounter(LogConfig.Interaction.AUTH_IMPRESSION, null)
    }
    fun showHistory() = show(AppNavDestination.HISTORY)
    fun showFeedback() = show(AppNavDestination.FEEDBACK)
    fun showFeedbackPreviewImage() = show(AppNavDestination.FEEDBACK_PREVIEW_IMAGE)

    /** Fires a Share Intent for the currently displayed page. */
    fun shareCurrentPage() {
        val activeTabModel = webLayerModel.currentBrowser.activeTabModel
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"

            putExtra(Intent.EXTRA_TEXT, activeTabModel.urlFlow.value.toString())
            putExtra(Intent.EXTRA_TITLE, activeTabModel.titleFlow.value)
        }

        safeStartActivityForIntent(Intent.createChooser(sendIntent, null))
    }

    fun onMenuItem(id: NeevaMenuItemId) {
        when (id) {
            NeevaMenuItemId.SPACES -> {
                openUrl(Uri.parse(NeevaConstants.appSpacesURL))
            }

            NeevaMenuItemId.SETTINGS -> {
                showSettings()
            }

            NeevaMenuItemId.HISTORY -> {
                showHistory()
            }

            NeevaMenuItemId.FORWARD -> {
                webLayerModel.currentBrowser.goForward()
            }

            NeevaMenuItemId.RELOAD -> {
                webLayerModel.currentBrowser.reload()
            }

            NeevaMenuItemId.SHOW_PAGE_INFO -> {
                webLayerModel.currentBrowser.showPageInfo()
            }

            NeevaMenuItemId.FIND_IN_PAGE -> {
                webLayerModel.currentBrowser.showFindInPage()
            }

            NeevaMenuItemId.FEEDBACK -> {
                showFeedback()
            }

            NeevaMenuItemId.UPDATE -> {
                openUrlViaIntent(NeevaConstants.playStoreUri)
            }

            NeevaMenuItemId.DOWNLOADS -> {
                safeStartActivityForIntent(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            }

            else -> {
                // Unimplemented screens.
            }
        }
    }

    companion object {
        val TAG = AppNavModel::class.simpleName
    }
}
