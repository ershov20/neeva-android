package com.neeva.app

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.window.layout.WindowMetricsCalculator
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.ActivityCallbacks
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.ContextMenuCreator
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.isSelected
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.firstrun.LocalFirstRunModel
import com.neeva.app.neeva_menu.LocalMenuData
import com.neeva.app.neeva_menu.LocalMenuDataState
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserToken
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    }

    @Inject lateinit var apolloWrapper: ApolloWrapper
    @Inject lateinit var spaceStore: SpaceStore
    @Inject lateinit var neevaUser: NeevaUser
    @Inject lateinit var snackbarModel: SnackbarModel
    @Inject lateinit var dispatchers: Dispatchers

    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var localEnvironmentState: LocalEnvironmentState

    private val activityViewModel: NeevaActivityViewModel by viewModels {
        NeevaActivityViewModel.Factory(intent)
    }

    internal val webLayerModel: WebLayerModel by viewModels()

    internal var appNavModel: AppNavModel? = null

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webLayerModel.activityCallbacks = WeakReference(this)

        setContentView(R.layout.main)

        findViewById<ComposeView>(R.id.browser_ui).apply {
            setContent {
                val navController = rememberAnimatedNavController()
                val isUpdateAvailable by activityViewModel.isUpdateAvailableFlow.collectAsState()
                val context = LocalContext.current

                appNavModel = remember(navController) {
                    AppNavModel(
                        context = context,
                        navController = navController,
                        webLayerModel = webLayerModel,
                        coroutineScope = lifecycleScope,
                        dispatchers = dispatchers,
                        snackbarModel = snackbarModel
                    )
                }

                NeevaTheme {
                    CompositionLocalProvider(
                        LocalEnvironment provides localEnvironmentState,
                        LocalAppNavModel provides appNavModel!!,
                        LocalFirstRunModel provides firstRunModel,
                        LocalMenuData provides LocalMenuDataState(
                            isUpdateAvailableVisible = isUpdateAvailable
                        )
                    ) {
                        ActivityUI(
                            bottomControlOffset = activityViewModel.bottomControlOffset,
                            topControlOffset = activityViewModel.topControlOffset,
                            webLayerModel = webLayerModel
                        )
                    }
                }

                LaunchedEffect(appNavModel) {
                    // Refresh the user's Spaces whenever they try to add something to one.
                    appNavModel?.currentDestination?.collect {
                        if (it?.route == AppNavDestination.ADD_TO_SPACE.name) {
                            spaceStore.refresh()
                        }
                    }
                }

                LaunchedEffect(true) {
                    if (firstRunModel.shouldShowFirstRun()) {
                        appNavModel?.showFirstRun()
                        firstRunModel.firstRunDone()
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.whenStarted {
                webLayerModel.initializationState
                    .combine(webLayerModel.browserWrapperFlow) { loadingState, browserWrapper ->
                        Pair(loadingState, browserWrapper)
                    }
                    .stateIn(lifecycleScope)
                    .collect { (loadingState, browserWrapper) ->
                        if (loadingState != LoadingState.READY) return@collect
                        prepareWebLayer(browserWrapper)

                        // Check if there are any Intents that have URLs that need to be loaded.
                        activityViewModel.getPendingLaunchIntent()?.let { processIntent(intent) }
                    }
            }
        }

        lifecycleScope.launch {
            fetchNeevaUserInfo()
        }

        lifecycleScope.launchWhenResumed {
            spaceStore.refresh()
        }

        if (savedInstanceState != null && webLayerModel.currentBrowser.isFullscreen()) {
            // If the activity was recreated because the user entered a fullscreen video or website,
            // hide the system bars.
            onEnterFullscreen()
        }
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
    }

    private fun processIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return

        if (Uri.parse(intent.dataString).scheme == "neeva") {
            NeevaUserToken.extractAuthTokenFromIntent(intent)?.let {
                neevaUser.neevaUserToken.setToken(it)
                webLayerModel.onAuthTokenUpdated()
                showBrowser()
                webLayerModel.currentBrowser.reload()
            }
        } else {
            intent.data?.let {
                webLayerModel.currentBrowser.loadUrl(
                    uri = it,
                    newTab = true,
                    isViaIntent = true
                )
            }
        }

        showBrowser()
    }

    private fun prepareWebLayer(browserWrapper: BrowserWrapper) {
        when {
            webLayerModel.initializationState.value != LoadingState.READY -> return
            isFinishing -> return
            isDestroyed -> return
        }

        val topControlPlaceholder = View(this)
        val bottomControlPlaceholder = View(this)
        webLayerModel.onWebLayerReady(
            browserWrapper,
            topControlPlaceholder,
            bottomControlPlaceholder,
            this::attachWebLayerFragment
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
        // Note the commitNow() instead of commit(). We want the fragment to get attached to
        // activity synchronously, so we can use all the functionality immediately. Otherwise we'd
        // have to wait until the commit is executed.
        val regularFragment = supportFragmentManager.findFragmentByTag(TAG_REGULAR_PROFILE)
        val incognitoFragment = supportFragmentManager.findFragmentByTag(TAG_INCOGNITO_PROFILE)

        val transaction = supportFragmentManager.beginTransaction()
        if (isIncognito) {
            regularFragment?.let { transaction.detach(it) }

            incognitoFragment
                ?.let { transaction.attach(it) }
                ?: run { transaction.add(R.id.weblayer_fragment, fragment, TAG_INCOGNITO_PROFILE) }
        } else {
            incognitoFragment?.let { transaction.detach(it) }

            regularFragment
                ?.let { transaction.attach(it) }
                ?: run { transaction.add(R.id.weblayer_fragment, fragment, TAG_REGULAR_PROFILE) }
        }

        transaction.commitNow()
    }

    override fun onBackPressed() {
        val browserWrapper = webLayerModel.currentBrowser

        when {
            browserWrapper.exitFullscreen() || browserWrapper.dismissTransientUi() == true -> {
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
                    webLayerModel.currentBrowser,
                    contextMenuParams,
                    tab,
                    context
                )
            )

            showContextMenu()
        }
    }

    override fun getDisplaySize(): Rect {
        return WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds
    }

    override fun onBottomBarOffsetChanged(offset: Int) =
        activityViewModel.onBottomBarOffsetChanged(offset)

    override fun onTopBarOffsetChanged(offset: Int) =
        activityViewModel.onTopBarOffsetChanged(offset)

    override fun detachIncognitoFragment() {
        // Do a post to avoid a Fragment transaction while one is already occurring to remove the
        // Incognito fragment, which can happen if the user closed all their incognito tabs
        // manually.
        Handler(Looper.getMainLooper()).post {
            supportFragmentManager.findFragmentByTag(TAG_INCOGNITO_PROFILE)?.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNow()
            }
        }
    }
}
