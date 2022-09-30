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
import android.util.Log
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
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.appnav.AppNavModelImpl
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
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.widget.NeevaWidgetProvider
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

@AndroidEntryPoint
class NeevaActivity : AppCompatActivity(), ActivityCallbacks {
    companion object {
        // Tags for the WebLayer Fragments.  Lets us retrieve them via the FragmentManager.
        private const val TAG_REGULAR_PROFILE = "FRAGMENT_TAG_REGULAR_PROFILE"
        private const val TAG_INCOGNITO_PROFILE = "FRAGMENT_TAG_INCOGNITO_PROFILE"

        /** Creates a lazy tab for the regular browser profile. */
        const val ACTION_NEW_TAB = "ACTION_NEW_TAB"

        /** Sends the user directly to the SpaceGrid. */
        const val ACTION_SHOW_SPACES = "ACTION_SHOW_SPACES"

        /** Sends the user directly into the Zero Query page without the keyboard up. */
        const val ACTION_ZERO_QUERY = "ACTION_ZERO_QUERY"

        private const val TAG = "NeevaActivity"
    }

    @Inject lateinit var activityCallbackProvider: ActivityCallbackProvider
    @Inject lateinit var apolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var clientLogger: ClientLogger
    @Inject lateinit var dispatchers: Dispatchers
    @Inject lateinit var domainProvider: DomainProvider
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var historyDatabase: HistoryDatabase
    @Inject lateinit var historyManager: HistoryManager
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser
    @Inject internal lateinit var settingsDataModel: SettingsDataModel
    @Inject lateinit var sharedPreferencesModel: SharedPreferencesModel
    @Inject lateinit var popupModel: PopupModel
    @Inject lateinit var spaceStore: SpaceStore

    internal val feedbackViewModel: FeedbackViewModel by viewModels()
    internal val webLayerModel: WebLayerModel by viewModels()
    private val zeroQueryViewModel: RegularProfileZeroQueryViewModel by viewModels()

    internal val activityViewModel: NeevaActivityViewModel by viewModels {
        NeevaActivityViewModel.Factory(
            intent,
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
                        popupModel = popupModel,
                        spaceStore = spaceStore,
                        onTakeScreenshot = this@NeevaActivity::takeScreenshotForFeedback,
                        neevaConstants = neevaConstants,
                        neevaUser = neevaUser
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
                        LocalSpaceStore provides spaceStore,
                        LocalRegularProfileZeroQueryViewModel provides zeroQueryViewModel
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val result = super.dispatchTouchEvent(ev)
        if (ev?.action == MotionEvent.ACTION_UP || ev?.action == MotionEvent.ACTION_DOWN) {
            webLayerModel.currentBrowser.resetOverscroll(ev.action)
        }
        return result
    }

    private fun showBrowser() = appNavModel?.showBrowser()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activityViewModel.determineScreenConfiguration(this)
    }

    override fun onStart() {
        super.onStart()
        activityViewModel.checkForUpdates(this)
        clientLogger.logCounter(LogConfig.Interaction.APP_ENTER_FOREGROUND, null)
        updateWidgets()
        webLayerModel.currentBrowser.reregisterActiveTabIfNecessary()
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

            ACTION_ZERO_QUERY -> {
                lifecycleScope.launch {
                    webLayerModel.switchToProfile(useIncognito = false)
                    webLayerModel.currentBrowser.waitUntilBrowserIsReady()
                    appNavModel?.openLazyTab(focusUrlBar = false)
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

                if (setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
                    clientLogger.logCounter(
                        LogConfig.Interaction.OPEN_DEFAULT_BROWSER_URL,
                        null
                    )
                }

                if (Uri.parse(intent.dataString).scheme == "neeva") {
                    LoginToken.extractAuthTokenFromIntent(intent)?.let {
                        neevaUser.loginToken.updateCachedCookie(it)
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
                Log.e(TAG, "Existing fragment found that does not match the new fragment")
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
