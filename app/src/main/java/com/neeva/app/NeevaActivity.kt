// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.app.SearchManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
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
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.appnav.AppNavModelImpl
import com.neeva.app.billing.SubscriptionManager
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.browsing.ActivityCallbackProvider
import com.neeva.app.browsing.ActivityCallbacks
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.LinkContextMenu
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.cardgrid.CardsPaneModelImpl
import com.neeva.app.cardgrid.SelectedScreen
import com.neeva.app.feedback.FeedbackViewModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsControllerImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.ui.PopupModel
import com.neeva.app.ui.removeViewFromParent
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.util.ScreenState
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.widget.NeevaWidgetProvider
import com.neeva.app.zeroquery.RateNeevaPromo.RateNeevaPromoModel
import com.neeva.app.zeroquery.RegularProfileZeroQueryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab
import org.chromium.weblayer.WebLayer
import timber.log.Timber

@AndroidEntryPoint
class NeevaActivity : AppCompatActivity(), ActivityCallbacks {
    companion object {
        // Tags for the WebLayer Fragments.  Lets us retrieve them via the FragmentManager.
        private const val TAG_REGULAR_PROFILE = "FRAGMENT_TAG_REGULAR_PROFILE"
        private const val TAG_INCOGNITO_PROFILE = "FRAGMENT_TAG_INCOGNITO_PROFILE"

        /** Creates a lazy tab for the regular browser profile. */
        const val ACTION_NEW_TAB = "ACTION_NEW_TAB"

        /**
         * Sends the user directly to a specified [AppNavDestination].
         * Should be used after a successful login.
         */
        const val ACTION_SHOW_SCREEN_AFTER_LOGIN = "ACTION_SHOW_SCREEN_AFTER_LOGIN"

        /** Sends the user directly to the SpaceGrid. */
        const val ACTION_SHOW_SPACES = "ACTION_SHOW_SPACES"

        /** Sends the user directly into the Zero Query page without the keyboard up. */
        const val ACTION_ZERO_QUERY = "ACTION_ZERO_QUERY"
    }

    @Inject lateinit var activityCallbackProvider: ActivityCallbackProvider
    @Inject lateinit var activityStarter: ActivityStarter
    @Inject lateinit var apolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var billingClientController: BillingClientController
    @Inject lateinit var clientLogger: ClientLogger
    @Inject lateinit var dispatchers: Dispatchers
    @Inject lateinit var domainProvider: DomainProvider
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var historyDatabase: HistoryDatabase
    @Inject lateinit var historyManager: HistoryManager
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var rateNeevaPromoModel: RateNeevaPromoModel
    @Inject internal lateinit var settingsDataModel: SettingsDataModel
    @Inject lateinit var sharedPreferencesModel: SharedPreferencesModel
    @Inject lateinit var popupModel: PopupModel
    @Inject lateinit var subscriptionManager: SubscriptionManager
    @Inject lateinit var spaceStore: SpaceStore
    @Inject lateinit var screenState: ScreenState

    internal val feedbackViewModel: FeedbackViewModel by viewModels()
    internal val webLayerModel: WebLayerModel by viewModels()
    private val zeroQueryViewModel: RegularProfileZeroQueryViewModel by viewModels()

    internal val activityViewModel: NeevaActivityViewModel by viewModels {
        NeevaActivityViewModel.Factory(
            neevaUser,
            spaceStore,
            webLayerModel,
            popupModel,
            firstRunModel,
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

        if (intent.action == Intent.ACTION_MAIN) {
            rateNeevaPromoModel.incrementLaunches()
        }

        activityCallbackProvider.activityCallbacks = WeakReference(this)

        activityViewModel.determineScreenConfiguration(this)
        if (savedInstanceState == null) {
            activityViewModel.pendingLaunchIntent = intent
        }

        setContentView(R.layout.main)

        findViewById<ComposeView>(R.id.browser_ui).apply {
            setContent {
                val navController = rememberAnimatedNavController()
                val context = LocalContext.current

                appNavModel = remember(navController) {
                    AppNavModelImpl(
                        activityStarter = activityStarter,
                        context = context,
                        navController = navController,
                        webLayerModel = webLayerModel,
                        coroutineScope = lifecycleScope,
                        dispatchers = dispatchers,
                        popupModel = popupModel,
                        spaceStore = spaceStore,
                        onTakeScreenshot = this@NeevaActivity::takeScreenshotForFeedback,
                        neevaConstants = neevaConstants,
                        neevaUser = neevaUser,
                        firstRunModel = firstRunModel
                    )
                }
                cardsPaneModel = remember(appNavModel) {
                    CardsPaneModelImpl(
                        context = context,
                        webLayerModel = webLayerModel,
                        appNavModel = appNavModel!!,
                        popupModel = popupModel,
                        coroutineScope = lifecycleScope
                    )
                }
                val settingsControllerImpl = remember(appNavModel) {
                    SettingsControllerImpl(
                        appNavModel = appNavModel!!,
                        settingsDataModel = settingsDataModel,
                        neevaUser = neevaUser,
                        neevaConstants = neevaConstants,
                        firstRunModel = firstRunModel,
                        subscriptionManager = subscriptionManager,
                        webLayerModel = webLayerModel,
                        onSignOut = activityViewModel::signOut,
                        setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
                        coroutineScope = lifecycleScope,
                        popupModel = popupModel,
                        activityCallbackProvider = activityCallbackProvider,
                        onTrackingProtectionUpdate = webLayerModel::updateBrowsersCookieCutterConfig
                    )
                }

                NeevaTheme {
                    CompositionLocalProvider(
                        LocalActivityStarter provides activityStarter,
                        LocalAppNavModel provides appNavModel!!,
                        LocalCardsPaneModel provides cardsPaneModel!!,
                        LocalChromiumVersion provides WebLayer.getSupportedFullVersion(context),
                        LocalClientLogger provides clientLogger,
                        LocalDispatchers provides dispatchers,
                        LocalDomainProvider provides domainProvider,
                        LocalFeedbackViewModel provides feedbackViewModel,
                        LocalFirstRunModel provides firstRunModel,
                        LocalHistoryManager provides historyManager,
                        LocalNavHostController provides navController,
                        LocalNeevaConstants provides neevaConstants,
                        LocalNeevaUser provides neevaUser,
                        LocalPopupModel provides popupModel,
                        LocalSettingsController provides settingsControllerImpl,
                        LocalSettingsDataModel provides settingsDataModel,
                        LocalSharedPreferencesModel provides sharedPreferencesModel,
                        LocalSubscriptionManager provides subscriptionManager,
                        LocalSpaceStore provides spaceStore,
                        LocalRegularProfileZeroQueryViewModel provides zeroQueryViewModel,
                        LocalScreenState provides screenState,
                        LocalRateNeevaPromoModel provides rateNeevaPromoModel,
                    ) {
                        ActivityUI(
                            toolbarConfiguration = activityViewModel.toolbarConfiguration,
                            webLayerModel = webLayerModel
                        )
                    }
                }

                LaunchedEffect(true) {
                    firstComposeCompleted.complete(true)
                }
            }
        }

        setDefaultAndroidBrowserManager =
            SetDefaultAndroidBrowserManager.create(this, neevaConstants, clientLogger)

        lifecycleScope.launch {
            // Keep track of when the BrowserWrapper changes so that the Activity can attach their
            // Fragments and create their Browsers.  This is important for catching when the
            // Activity is recreated due to a configuration change, like entering fullscreen and
            // automatically switching to landscape when watching a video.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                webLayerModel.initializedBrowserFlow.collectLatest { prepareBrowser(it) }
            }
        }

        lifecycleScope.launchWhenResumed {
            withContext(dispatchers.io) {
                spaceStore.refresh()
            }
        }

        lifecycleScope.launchWhenResumed {
            if (!SharedPrefFolder.App.RequestedInstallReferrer.get(sharedPreferencesModel)) {
                activityViewModel.requestInstallReferrer(this@NeevaActivity, clientLogger)
                SharedPrefFolder.App.RequestedInstallReferrer.set(
                    sharedPreferencesModel, true
                )
            }
        }

        // TODO(kobec): remove when Billing is ready
        if (settingsDataModel.getSettingsToggleValue(SettingsToggle.DEBUG_ENABLE_BILLING)) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    billingClientController.onResume()
                }
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
        popupModel.dismissSnackbar()
        webLayerModel.currentBrowser.takeScreenshotOfActiveTab()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClientController.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val result = super.dispatchTouchEvent(ev)
        if (ev?.action == MotionEvent.ACTION_UP || ev?.action == MotionEvent.ACTION_DOWN) {
            webLayerModel.currentBrowser.resetOverscroll(ev.action)
        }
        return result
    }

    private fun showBrowser(forceUserToStayInCardGrid: Boolean = true) {
        lifecycleScope.launch(dispatchers.main) {
            // Speculative fix for https://github.com/neevaco/neeva-android/issues/939
            // Wait until the first Compose has completed, which should mean that the NavGraph has
            // been set up with all of the destinations.
            firstComposeCompleted.await()
            appNavModel?.showBrowser(forceUserToStayInCardGrid = forceUserToStayInCardGrid)
        }
    }

    private fun showSettings() {
        lifecycleScope.launch(dispatchers.main) {
            // Speculative fix for https://github.com/neevaco/neeva-android/issues/939
            // Wait until the first Compose has completed, which should mean that the NavGraph has
            // been set up with all of the destinations.
            firstComposeCompleted.await()
            appNavModel?.showSettings()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activityViewModel.determineScreenConfiguration(this)
        screenState.configure(this)
    }

    override fun onStart() {
        super.onStart()

        activityViewModel.checkForUpdates(this)
        clientLogger.logCounter(LogConfig.Interaction.APP_ENTER_FOREGROUND, null)
        updateWidgets()

        // If the user isn't in the tab switcher when the app is brought back to the foreground,
        // force a tab to be set as active.
        val userIsViewingTabSwitcher =
            appNavModel?.currentDestination?.value?.route == AppNavDestination.CARD_GRID?.route
        webLayerModel.currentBrowser.onActivityStart(allowNullActiveTab = userIsViewingTabSwitcher)
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
    private fun processIntent(intent: Intent?) = lifecycleScope.launch(dispatchers.main) {
        // Wait until the NavGraph has been set up.
        firstComposeCompleted.await()

        val switchToRegularProfile = suspend {
            webLayerModel.switchToProfile(useIncognito = false)
            webLayerModel.currentBrowser.waitUntilBrowserIsReady()
        }

        var uriToLoad: Uri? = null
        var onLoadStarted = { showBrowser(forceUserToStayInCardGrid = false) }

        when (intent?.action) {
            ACTION_NEW_TAB -> {
                switchToRegularProfile()
                appNavModel?.openLazyTab()
            }

            ACTION_ZERO_QUERY -> {
                switchToRegularProfile()
                appNavModel?.openLazyTab(focusUrlBar = false)
            }

            ACTION_SHOW_SPACES -> {
                switchToRegularProfile()
                cardsPaneModel?.switchScreen(SelectedScreen.SPACES)
                appNavModel?.showCardGrid()
            }

            ACTION_SHOW_SCREEN_AFTER_LOGIN -> {
                switchToRegularProfile()
                processScreenToNavigateTo()
                uriToLoad = intent.data
                // Set onLoadStarted to an empty lambda so that the tab is opened by the user is
                // sent to the correct screen.
                onLoadStarted = { }
            }

            Intent.ACTION_VIEW -> {
                switchToRegularProfile()
                // Logs if the app is set as the system's default browser when we are asked to
                // handle a VIEW Intent.
                if (setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
                    clientLogger.logCounter(LogConfig.Interaction.OPEN_DEFAULT_BROWSER_URL, null)
                }
                uriToLoad = intent.data
            }

            Intent.ACTION_WEB_SEARCH -> {
                switchToRegularProfile()
                uriToLoad = intent.extras
                    ?.getString(SearchManager.QUERY)
                    ?.toSearchUri(neevaConstants)
            }

            // Don't know what to do with it.
            else -> {}
        }

        uriToLoad?.let {
            webLayerModel.currentBrowser.loadUrl(
                uri = it,
                inNewTab = true,
                isViaIntent = true,
                onLoadStarted = onLoadStarted
            )
        }
    }

    private fun processScreenToNavigateTo() {
        // In case the user signed in from the SpacesIntro Bottom Sheet, remove the bottom sheet
        // since the user has successfully signed in.
        popupModel.removeBottomSheet()

        val screenToReturnTo = firstRunModel.getScreenToReturnToAfterLogin()
        firstRunModel.clearDestinationsToReturnAfterLogin()
        when (screenToReturnTo) {
            AppNavDestination.SETTINGS.name -> showSettings()
            else -> showBrowser()
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
        activityViewModel.pendingLaunchIntent?.let { processIntent(it) }

        isBrowserPreparedFlow.value = true
    }

    private fun takeScreenshotForFeedback(callback: () -> Unit) {
        val isBrowserVisible = when (appNavModel?.currentDestination?.value?.route) {
            AppNavDestination.BROWSER.route -> true
            else -> false
        }

        feedbackViewModel.takeScreenshot(
            isBrowserVisible = isBrowserVisible,
            window = window,
            currentBrowser = webLayerModel.currentBrowser,
            callback = callback
        )
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
        otherProfileFragment?.apply {
            transaction.detach(this)

            // https://github.com/neevaco/neeva-android/issues/571
            // Because we have to manually move the Fragment's View into the Composable hierarchy in
            // the BrowserScaffold, we have to manually remove it from the hierarchy when we detach
            // its Fragment.
            removeViewFromParent(view)
        }

        if (existingFragment != null) {
            if (fragment != existingFragment) {
                // https://github.com/neevaco/neeva-android/issues/940
                // It's not clear what causes two different Fragments to exist for the same browser
                // profile.  Maybe two different Incognito fragments, or maybe something is
                // happening after an Activity restart, but I haven't been able to reproduce it.
                // Speculatively avoid the crash by removing the wrong fragment when a new Fragment
                // is added for the same profile.
                Timber.e("Existing fragment found that does not match the new fragment")
                transaction.remove(existingFragment)
                transaction.add(R.id.weblayer_fragment, fragment, fragmentTag)
            } else {
                // Re-attach the Fragment so that WebLayer knows that it is now active.
                transaction.attach(fragment)
            }
        } else {
            transaction.add(R.id.weblayer_fragment, fragment, fragmentTag)
        }

        // Note the commitNow() instead of commit(). We want the fragment to get attached to
        // activity synchronously, so we can use all the functionality immediately. Otherwise we'd
        // have to wait until the commit is executed.
        transaction.commitNow()
    }

    @Suppress("DEPRECATION")
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
                appNavModel?.navigateBackOnActiveTab()
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

        val intent = Intent(this, this@NeevaActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        startActivity(intent)
    }

    override fun showContextMenuForTab(contextMenuParams: ContextMenuParams, tab: Tab) {
        popupModel.showContextMenu { onDismissRequested ->
            LinkContextMenu(
                webLayerModel = webLayerModel,
                params = contextMenuParams,
                tab = tab,
                onDismissRequested = onDismissRequested
            )
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
                fragment.view?.let { removeViewFromParent(it) }

                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNow()
            }
        }
    }

    override fun fireExternalIntentForUri(uri: Uri, closeTabIfSuccessful: Boolean) =
        activityViewModel.fireExternalIntentForUri(this, uri, closeTabIfSuccessful)

    /** When activated, asks the user to select a file containing a previously exported database. */
    private val importDatabase = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let {
            lifecycleScope.launch(dispatchers.io) {
                HistoryDatabase.prepareDatabaseForImport(
                    context = this@NeevaActivity,
                    contentUri = it,
                    sharedPreferencesModel = sharedPreferencesModel
                )
            }
        }
    }

    /**
     * Asks the user to select a file containing a previously exported database and overwrites their
     * existing database with it.
     */
    override fun importHistoryDatabase() {
        importDatabase.launch(arrayOf("application/zip"))
    }

    override fun exportHistoryDatabase() {
        lifecycleScope.launch {
            historyDatabase.export(
                context = this@NeevaActivity,
                dispatchers = dispatchers
            )
        }
    }
}
