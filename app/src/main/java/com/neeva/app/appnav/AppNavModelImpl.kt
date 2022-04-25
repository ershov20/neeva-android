package com.neeva.app.appnav

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.neeva.app.Dispatchers
import com.neeva.app.LocalEnvironment
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.spaces.AddToSpaceUI
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.ui.widgets.overlay.OverlaySheetModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AppNavModelImpl(
    private val context: Context,
    override val navController: NavHostController,
    private val webLayerModel: WebLayerModel,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val overlaySheetModel: OverlaySheetModel,
    private val snackbarModel: SnackbarModel,
    private val clientLogger: ClientLogger,
    private val onTakeScreenshot: (callback: () -> Unit) -> Unit
) : AppNavModel {
    private val _currentDestination = MutableStateFlow(navController.currentDestination)
    override val currentDestination: StateFlow<NavDestination?>
        get() = _currentDestination

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

    override fun popBackStack() {
        navController.popBackStack()
    }

    /**
     * Show the browser view of the app.
     *
     * If the user has no tabs open, they are instead sent to the tab switcher unless
     * [forceUserToStayInCardGrid] is set to false.
     */
    override fun showBrowser(forceUserToStayInCardGrid: Boolean) {
        webLayerModel.currentBrowser.urlBarModel.clearFocus()

        navController.popBackStack(
            route = AppNavDestination.BROWSER.route,
            inclusive = false
        )

        if (webLayerModel.currentBrowser.userMustBeShownCardGrid() && forceUserToStayInCardGrid) {
            showCardGrid()
        }
    }

    override fun openLazyTab() {
        // Ordering is important here because showing the browser clears the focus of the URL bar
        // while opening a lazy tab requests the focus on the URL bar.
        showBrowser(forceUserToStayInCardGrid = false)
        webLayerModel.currentBrowser.openLazyTab()
    }

    override fun openUrl(url: Uri) {
        webLayerModel.currentBrowser.loadUrl(
            uri = url,
            inNewTab = true,
            onLoadStarted = this::showBrowser,
            stayInApp = true
        )
    }

    override fun openAndroidDefaultBrowserSettings() {
        safeStartActivityForIntent(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }

    override fun openUrlViaIntent(uri: Uri) {
        safeStartActivityForIntent(Intent(Intent.ACTION_VIEW, uri))
        showBrowser()
    }

    override fun safeStartActivityForIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            snackbarModel.show(context.getString(R.string.error_generic))
            Log.e(TAG, "Failed to start Activity for $intent")
        }
    }

    override fun showCardGrid() = show(AppNavDestination.CARD_GRID)
    override fun showSettings() = show(AppNavDestination.SETTINGS)
    override fun showProfileSettings() = show(AppNavDestination.PROFILE_SETTINGS)
    override fun showClearBrowsingSettings() = show(AppNavDestination.CLEAR_BROWSING_SETTINGS)
    override fun showDefaultBrowserSettings() = show(AppNavDestination.SET_DEFAULT_BROWSER_SETTINGS)
    override fun showLocalFeatureFlagsPane() = show(AppNavDestination.LOCAL_FEATURE_FLAGS_SETTINGS)

    override fun showSignUpLanding() {
        show(AppNavDestination.SIGN_UP_LANDING_PAGE)
        clientLogger.logCounter(LogConfig.Interaction.AUTH_IMPRESSION_LANDING, null)
    }

    override fun showSignUpWithOther() {
        show(AppNavDestination.SIGN_UP_OTHER)
        clientLogger.logCounter(LogConfig.Interaction.AUTH_IMPRESSION_OTHER, null)
    }

    override fun showSignIn() {
        show(AppNavDestination.SIGN_IN)
        clientLogger.logCounter(LogConfig.Interaction.AUTH_IMPRESSION_SIGN_IN, null)
    }

    override fun showHistory() = show(AppNavDestination.HISTORY)

    override fun showFeedback() {
        onTakeScreenshot {
            show(AppNavDestination.FEEDBACK)
        }
    }

    override fun showHelp() {
        openUrl(Uri.parse(NeevaConstants.appHelpCenterURL))
    }

    override fun shareCurrentPage() {
        val activeTabModel = webLayerModel.currentBrowser.activeTabModel
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"

            putExtra(Intent.EXTRA_TEXT, activeTabModel.urlFlow.value.toString())
            putExtra(Intent.EXTRA_TITLE, activeTabModel.titleFlow.value)
        }

        safeStartActivityForIntent(Intent.createChooser(sendIntent, null))
    }

    override fun showAddToSpace() {
        overlaySheetModel.showOverlaySheet(titleResId = R.string.toolbar_save_to_space) {
            val spaceStore = LocalEnvironment.current.spaceStore
            val browserWrapper = webLayerModel.currentBrowser
            val activeTabModel = browserWrapper.activeTabModel

            LaunchedEffect(true) {
                spaceStore.refresh()
            }

            AddToSpaceUI(activeTabModel, spaceStore) { space ->
                browserWrapper.modifySpace(space.id)
                overlaySheetModel.hideOverlaySheet()
            }
        }
    }

    override fun onMenuItem(id: NeevaMenuItemId) {
        when (id) {
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

            NeevaMenuItemId.TOGGLE_DESKTOP_SITE -> {
                webLayerModel.currentBrowser.toggleViewDesktopSite()
            }

            NeevaMenuItemId.SUPPORT -> {
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

    override fun debugOpenManyTabs(numTabs: Int) {
        coroutineScope.launch {
            val possibleUrls = listOf(
                "https://en.wikipedia.org",
                "https://youtube.com",
                "https://amazon.com",
                "https://facebook.com",
                "https://twitter.com",
                "https://fandom.com",
                "https://pinterest.com",
                "https://imdb.com",
                "https://reddit.com",
                "https://yelp.com",
                "https://instagram.com",
                "https://ebay.com",
                "https://walmart.com",
                "https://craigslist.org",
                "https://healthline.com",
                "https://tripadvisor.com",
                "https://linkedin.com",
                "https://webmd.com",
                "https://netflix.com",
                "https://apple.com",
                "https://homedepot.com",
                "https://mail.yahoo.com",
                "https://cnn.com",
                "https://etsy.com",
                "https://google.com",
                "https://yahoo.com",
                "https://indeed.com",
                "https://target.com",
                "https://microsoft.com",
                "https://nytimes.com",
                "https://mayoclinic.org",
                "https://espn.com",
                "https://usps.com",
                "https://quizlet.com",
                "https://gamepedia.com",
                "https://lowes.com",
                "https://irs.gov",
                "https://nih.gov",
                "https://merriam-webster.com",
                "https://steampowered.com"
            )
            for (i in 0 until numTabs) {
                openUrl(Uri.parse(possibleUrls[i % possibleUrls.size]))
                delay(250)
            }
            snackbarModel.show("Opened $numTabs tabs")
        }
    }

    companion object {
        val TAG = AppNavModelImpl::class.simpleName
    }
}
