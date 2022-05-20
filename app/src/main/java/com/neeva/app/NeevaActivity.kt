package com.neeva.app

import android.app.SearchManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.WindowMetricsCalculator
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.appnav.AppNavModelImpl
import com.neeva.app.browsing.ActivityCallbackProvider
import com.neeva.app.browsing.ActivityCallbacks
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.ContextMenuCreator
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.isSelected
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.cardgrid.CardsPaneModelImpl
import com.neeva.app.cardgrid.SelectedScreen
import com.neeva.app.feedback.FeedbackViewModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.firstrun.LocalFirstRunModel
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.SettingsControllerImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.ui.removeViewFromParent
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.overlay.OverlaySheetModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserToken
import com.neeva.app.widget.NeevaWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab

@AndroidEntryPoint
class NeevaActivity : AppCompatActivity(), ActivityCallbacks {
    companion object {
        // Tags for the WebLayer Fragments.  Lets us retrieve them via the FragmentManager.
        private const val TAG_REGULAR_PROFILE = "FRAGMENT_TAG_REGULAR_PROFILE"
        private const val TAG_INCOGNITO_PROFILE = "FRAGMENT_TAG_INCOGNITO_PROFILE"

        const val ACTION_NEW_TAB = "ACTION_NEW_TAB"
        const val ACTION_SHOW_SPACES = "ACTION_SHOW_SPACES"
    }

    @Inject lateinit var activityCallbackProvider: ActivityCallbackProvider
    @Inject lateinit var apolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var dispatchers: Dispatchers
    @Inject lateinit var historyDatabase: HistoryDatabase
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var overlaySheetModel: OverlaySheetModel
    @Inject internal lateinit var settingsDataModel: SettingsDataModel
    @Inject lateinit var snackbarModel: SnackbarModel
    @Inject lateinit var spaceStore: SpaceStore

    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var localEnvironmentState: LocalEnvironmentState

    @Inject lateinit var clientLogger: ClientLogger

    private val feedbackViewModel: FeedbackViewModel by viewModels()
    internal val webLayerModel: WebLayerModel by viewModels()

    internal val activityViewModel: NeevaActivityViewModel by viewModels {
        NeevaActivityViewModel.Factory(
            intent,
            neevaUser,
            spaceStore,
            webLayerModel,
            snackbarModel,
            dispatchers
        )
    }

    internal var appNavModel: AppNavModel? = null
    internal var cardsPaneModel: CardsPaneModel? = null
    internal val firstComposeCompleted = CompletableDeferred<Boolean>()

    private lateinit var setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager

    @VisibleForTesting
    internal val isBrowserPreparedFlow = mutableStateOf(false)

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCallbackProvider.activityCallbacks = WeakReference(this)

        activityViewModel.determineScreenConfiguration(this)
        setContentView(R.layout.main)

        findViewById<ComposeView>(R.id.browser_ui).apply {
            setContent {
                val navController = rememberAnimatedNavController()
                val context = LocalContext.current

                appNavModel = remember(navController) {
                    AppNavModelImpl(
                        context = context,
                        navController = navController,
                        webLayerModel = webLayerModel,
                        coroutineScope = lifecycleScope,
                        dispatchers = dispatchers,
                        overlaySheetModel = overlaySheetModel,
                        spaceStore = spaceStore,
                        snackbarModel = snackbarModel,
                        clientLogger = clientLogger,
                        onTakeScreenshot = this@NeevaActivity::takeScreenshotForFeedback,
                        neevaConstants = neevaConstants
                    )
                }
                cardsPaneModel = remember(appNavModel) {
                    CardsPaneModelImpl(
                        webLayerModel = webLayerModel,
                        appNavModel = appNavModel!!,
                        overlaySheetModel = overlaySheetModel,
                        settingsDataModel = localEnvironmentState.settingsDataModel,
                        coroutineScope = lifecycleScope
                    )
                }
                val settingsControllerImpl = remember(appNavModel) {
                    SettingsControllerImpl(
                        context = context,
                        appNavModel = appNavModel!!,
                        settingsDataModel = localEnvironmentState.settingsDataModel,
                        neevaUser = localEnvironmentState.neevaUser,
                        webLayerModel = webLayerModel,
                        onSignOut = activityViewModel::signOut,
                        setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
                        coroutineScope = lifecycleScope,
                        dispatchers = dispatchers,
                        snackbarModel = localEnvironmentState.snackbarModel,
                        historyDatabase = historyDatabase,
                        onTrackingProtectionUpdate = webLayerModel::updateBrowsersCookieCutterConfig
                    )
                }

                NeevaTheme {
                    CompositionLocalProvider(
                        LocalAppNavModel provides appNavModel!!,
                        LocalCardsPaneModel provides cardsPaneModel!!,
                        LocalEnvironment provides localEnvironmentState,
                        LocalFeedbackViewModel provides feedbackViewModel,
                        LocalFirstRunModel provides firstRunModel,
                        LocalNavHostController provides navController,
                        LocalSettingsController provides settingsControllerImpl
                    ) {
                        ActivityUI(
                            toolbarConfiguration = activityViewModel.toolbarConfiguration,
                            webLayerModel = webLayerModel
                        )
                    }
                }

                LaunchedEffect(true) {
                    if (firstRunModel.shouldShowFirstRun()) {
                        appNavModel?.showSignInFlow()
                        clientLogger.logCounter(LogConfig.Interaction.FIRST_RUN_IMPRESSION, null)
                        firstRunModel.firstRunDone()
                    }

                    firstComposeCompleted.complete(true)
                }
            }
        }

        setDefaultAndroidBrowserManager =
            SetDefaultAndroidBrowserManager.create(this, neevaConstants)

        lifecycleScope.launch {
            // Keep track of when the BrowserWrapper changes so that the Activity can attach their
            // Fragments and create their Browsers.  This is important for catching when the
            // Activity is recreated due to a configuration change, like entering fullscreen and
            // automatically switching to landscape when watching a video.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                webLayerModel.initializedBrowserFlow.collectLatest { prepareBrowser(it) }
            }
        }

        lifecycleScope.launch {
            fetchNeevaUserInfo()
        }

        lifecycleScope.launchWhenResumed {
            withContext(dispatchers.io) {
                spaceStore.refresh()
            }
        }

        if (savedInstanceState != null && webLayerModel.currentBrowser.isFullscreen()) {
            // If the activity was recreated because the user entered a fullscreen video or website,
            // hide the system bars.
            onEnterFullscreen()
        }

        // Keep track of whether the keyboard is open so that we can synchronize UI across both
        // WebLayer (with its Android Views) and Compose (with its own Composable hierarchy).
        val rootView: View = findViewById(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(rootView)
            val isKeyboardOpen = insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
            activityViewModel.onKeyboardStateChanged(isKeyboardOpen)
        }
    }

    override fun onPause() {
        webLayerModel.currentBrowser.takeScreenshotOfActiveTab()
        super.onPause()
    }

    private fun showBrowser() = appNavModel?.showBrowser()

    private suspend fun fetchNeevaUserInfo() {
        withContext(dispatchers.io) {
            neevaUser.fetch(apolloWrapper)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        activityViewModel.checkForUpdates(this)
        clientLogger.logCounter(LogConfig.Interaction.APP_ENTER_FOREGROUND, null)
        updateWidgets()
    }

    private fun updateWidgets() {
        val componentName = ComponentName(this, NeevaWidgetProvider::class.java)
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        ids.forEach { id ->
            NeevaWidgetProvider.updateWidget(
                context = this,
                appWidgetManager = appWidgetManager,
                appWidgetId = id,
                appURL = neevaConstants.appURL
            )
        }
    }

    /** Processes the given [intent] when everything is initialized. */
    private fun processIntent(intent: Intent?) {
        lifecycleScope.launch {
            firstComposeCompleted.await()
            processIntentInternal(intent)
        }
    }

    /** Don't call this: call [processIntent] to ensure we're ready before processing the Intent. */
    private fun processIntentInternal(intent: Intent?) {
        when (intent?.action) {
            ACTION_NEW_TAB -> {
                lifecycleScope.launch {
                    webLayerModel.switchToProfile(useIncognito = false)
                    webLayerModel.currentBrowser.waitUntilBrowserIsReady()
                    appNavModel?.openLazyTab()
                }
            }

            ACTION_SHOW_SPACES -> {
                lifecycleScope.launch {
                    webLayerModel.currentBrowser.waitUntilBrowserIsReady()

                    // Switching to spaces will automatically kick the user out of Incognito.
                    cardsPaneModel?.switchScreen(SelectedScreen.SPACES)
                    appNavModel?.showCardGrid()
                }
            }

            Intent.ACTION_VIEW -> {
                webLayerModel.switchToProfile(useIncognito = false)

                if (Uri.parse(intent.dataString).scheme == "neeva") {
                    NeevaUserToken.extractAuthTokenFromIntent(intent)?.let {
                        neevaUser.neevaUserToken.setToken(it)
                        webLayerModel.onAuthTokenUpdated()
                        showBrowser()
                        webLayerModel.currentBrowser.reload()
                        if (firstRunModel.shouldLogFirstLogin()) {
                            clientLogger.logCounter(
                                LogConfig.Interaction.LOGIN_AFTER_FIRST_RUN, null
                            )
                            firstRunModel.setShouldLogFirstLogin(false)
                        }
                    }
                } else {
                    intent.data?.let {
                        webLayerModel.currentBrowser.loadUrl(
                            uri = it,
                            inNewTab = true,
                            isViaIntent = true,
                            onLoadStarted = this::showBrowser
                        )
                    }
                }
            }

            Intent.ACTION_WEB_SEARCH -> {
                webLayerModel.switchToProfile(useIncognito = false)

                intent.extras?.getString(SearchManager.QUERY)?.let {
                    val searchUri = it.toSearchUri(neevaConstants)

                    webLayerModel.currentBrowser.loadUrl(
                        uri = searchUri,
                        inNewTab = true,
                        isViaIntent = true,
                        onLoadStarted = this::showBrowser
                    )
                }
            }

            // Don't know what to do with it.
            else -> {}
        }
    }

    private fun prepareBrowser(browserWrapper: BrowserWrapper) {
        when {
            isFinishing -> return
            isDestroyed -> return
        }

        // Hide the contents of the screen from Android Recents when the user switches to another
        // app while looking at an Incognito tab.
        val enableIncognitoScreenshots = settingsDataModel
            .getSettingsToggleValue(SettingsToggle.DEBUG_ENABLE_INCOGNITO_SCREENSHOTS)
        if (browserWrapper.isIncognito && !enableIncognitoScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val displaySize =
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds
        browserWrapper.createAndAttachBrowser(
            displaySize,
            activityViewModel.toolbarConfiguration,
            this::attachWebLayerFragment
        )

        // Check if there are any Intents that have URLs that need to be loaded.
        activityViewModel.getPendingLaunchIntent()?.let { processIntent(it) }

        isBrowserPreparedFlow.value = true
    }

    private fun takeScreenshotForFeedback(callback: () -> Unit) {
        feedbackViewModel.takeScreenshot(window, webLayerModel.currentBrowser, callback)
    }

    override fun getWebLayerFragment(isIncognito: Boolean): Fragment? {
        return supportFragmentManager.findFragmentByTag(
            when {
                isIncognito -> TAG_INCOGNITO_PROFILE
                else -> TAG_REGULAR_PROFILE
            }
        )
    }

    /**
     * Attach the given [Fragment] to the Activity, which allows creation of the [Browser].
     *
     * If the user is swapping between Profiles, the Fragment associated with the previous Profile
     * is detached to prevent WebLayer from restarting/reshowing the Fragment automatically when the
     * app is foregrounded again.
     */
    private fun attachWebLayerFragment(fragment: Fragment, isIncognito: Boolean) {
        // Look for any existing WebLayer fragments.  Fragments may already exist if:
        // * The user swapped to the other Browser profile and then back
        // * The Activity died in the background or it was recreated because of a config change
        val regularFragment = getWebLayerFragment(isIncognito = false)
        val incognitoFragment = getWebLayerFragment(isIncognito = true)

        val otherProfileFragment: Fragment?
        val existingFragment: Fragment?
        val fragmentTag: String

        if (isIncognito) {
            otherProfileFragment = regularFragment
            existingFragment = incognitoFragment
            fragmentTag = TAG_INCOGNITO_PROFILE
        } else {
            otherProfileFragment = incognitoFragment
            existingFragment = regularFragment
            fragmentTag = TAG_REGULAR_PROFILE
        }

        val transaction = supportFragmentManager.beginTransaction()

        // Detach the Fragment for the other profile so that WebLayer knows that the user isn't
        // actively using that Profile.  This also prevents WebLayer from automatically reshowing
        // the Browser attached to that Fragment as soon as the Activity starts up.
        otherProfileFragment?.let { fragment ->
            transaction.detach(fragment)

            // https://github.com/neevaco/neeva-android/issues/571
            // Because we have to manually move the Fragment's View into the Composable hierarchy in
            // the BrowserScaffold, we have to manually remove it from the hierarchy when we detach
            // its Fragment.
            removeViewFromParent(fragment.view)
        }

        if (existingFragment != null) {
            // Re-attach the Fragment so that WebLayer knows that it is now active.
            check(fragment == existingFragment)
            transaction.attach(fragment)
        } else {
            transaction.add(R.id.weblayer_fragment, fragment, fragmentTag)
        }

        // Note the commitNow() instead of commit(). We want the fragment to get attached to
        // activity synchronously, so we can use all the functionality immediately. Otherwise we'd
        // have to wait until the commit is executed.
        transaction.commitNow()
    }

    override fun onBackPressed() {
        val browserWrapper = webLayerModel.currentBrowser
        when {
            browserWrapper.exitFullscreen() || browserWrapper.dismissTransientUi() -> {
                return
            }

            onBackPressedDispatcher.hasEnabledCallbacks() -> {
                onBackPressedDispatcher.onBackPressed()
            }

            browserWrapper.canGoBackward() -> {
                browserWrapper.goBack()
            }

            browserWrapper.closeActiveChildTab() -> {
                // Closing the child tab will kick the user back to the parent tab, if possible.
                return
            }

            browserWrapper.closeActiveTabIfOpenedViaIntent() -> {
                // Let Android kick the user back to the calling app.
                super.onBackPressed()
            }

            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onEnterFullscreen() {
        val rootView: View = findViewById(android.R.id.content)
        rootView.keepScreenOn = true

        val controller = WindowInsetsControllerCompat(window, rootView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onExitFullscreen() {
        val rootView: View = findViewById(android.R.id.content)
        rootView.keepScreenOn = false

        val controller = WindowInsetsControllerCompat(window, rootView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun bringToForeground() {
        showBrowser()

        val intent = Intent(this, NeevaActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        startActivity(intent)
    }

    override fun showContextMenuForTab(contextMenuParams: ContextMenuParams, tab: Tab) {
        if (tab.isDestroyed || !tab.isSelected) return

        findViewById<View>(R.id.weblayer_fragment)?.apply {
            // Need to use the NeevaActivity as the context because the WebLayer View doesn't have
            // access to the correct resources.
            setOnCreateContextMenuListener(
                ContextMenuCreator(
                    webLayerModel,
                    contextMenuParams,
                    tab,
                    context
                )
            )

            showContextMenu()
        }
    }

    override fun onBottomBarOffsetChanged(offset: Int) =
        activityViewModel.onBottomBarOffsetChanged(offset)

    override fun onTopBarOffsetChanged(offset: Int) =
        activityViewModel.onTopBarOffsetChanged(offset)

    override fun removeIncognitoFragment() {
        // Because this may be triggered by another FragmentManager transaction, do a post to avoid
        // possibly nesting FragmentManager transactions (and causing a crash).
        Handler(Looper.getMainLooper()).post {
            getWebLayerFragment(isIncognito = true)?.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNow()
            }
        }
    }

    override fun fireExternalIntentForUri(uri: Uri, closeTabIfSuccessful: Boolean) =
        activityViewModel.fireExternalIntentForUri(this, uri, closeTabIfSuccessful)
}
