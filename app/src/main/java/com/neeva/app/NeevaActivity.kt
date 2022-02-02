package com.neeva.app

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.window.layout.WindowMetricsCalculator
import com.apollographql.apollo3.ApolloClient
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.neeva.app.browsing.ActivityCallbacks
import com.neeva.app.browsing.ContextMenuCreator
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.settings.SettingsModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.NeevaUser
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab

@AndroidEntryPoint
class NeevaActivity : AppCompatActivity(), ActivityCallbacks {
    companion object {
        private const val EXTRA_START_IN_INCOGNITO = "EXTRA_START_IN_INCOGNITO"
        private const val TAG_REGULAR_PROFILE = "FRAGMENT_TAG_REGULAR_PROFILE"
        private const val TAG_INCOGNITO_PROFILE = "FRAGMENT_TAG_INCOGNITO_PROFILE"
    }

    @Inject lateinit var apolloClient: ApolloClient
    @Inject lateinit var spaceStore: SpaceStore
    @Inject lateinit var settingsModel: SettingsModel
    @Inject lateinit var sharedPreferencesModel: SharedPreferencesModel
    @Inject lateinit var neevaUserToken: NeevaUserToken
    @Inject lateinit var webModel: WebLayerModel
    @Inject lateinit var historyManager: HistoryManager
    @Inject lateinit var dispatchers: Dispatchers

    private lateinit var containerRegularProfile: View
    private lateinit var containerIncognitoProfile: View

    /**
     * WebLayer provides information about when the bottom and top toolbars need to be scrolled off.
     * We provide a placeholder instead of the real view because WebLayer has a bug that prevents it
     * from rendering Composables properly.
     * TODO(dan.alcantara): Revisit this once we move past WebLayer/Chromium v96.
     */
    private val topControlOffset = MutableStateFlow(0.0f)
    private val bottomControlOffset = MutableStateFlow(0.0f)

    private var appNavModel: AppNavModel? = null

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webModel.activityCallbacks = WeakReference(this)

        setContentView(R.layout.main)

        findViewById<ComposeView>(R.id.browser_ui).apply {
            setContent {
                val navController = rememberAnimatedNavController()

                appNavModel = remember(navController) {
                    AppNavModel(
                        navController = navController,
                        webLayerModel = webModel,
                        coroutineScope = lifecycleScope,
                        dispatchers = dispatchers
                    )
                }

                LaunchedEffect(appNavModel) {
                    // Refresh the user's Spaces whenever they try to add something to one.
                    appNavModel?.currentDestination?.collect {
                        if (it?.route == AppNavDestination.ADD_TO_SPACE.name) {
                            spaceStore.refresh()
                        }
                    }
                }

                ActivityUI(
                    browserWrapperFlow = webModel.browserWrapperFlow,
                    bottomControlOffset = bottomControlOffset,
                    topControlOffset = topControlOffset,
                    appNavModel = appNavModel!!,
                    webLayerModel = webModel,
                    settingsModel = settingsModel,
                    apolloClient = apolloClient,
                    historyManager = historyManager,
                    dispatchers = dispatchers,
                    sharedPreferencesModel = sharedPreferencesModel,
                    neevaUserToken = neevaUserToken
                )
            }
        }

        containerRegularProfile = findViewById(R.id.weblayer_regular)
        containerIncognitoProfile = findViewById(R.id.weblayer_incognito)

        lifecycleScope.launch {
            lifecycle.whenStarted {
                webModel.initializationState
                    .combine(webModel.browserWrapperFlow) { loadingState, browserDelegate ->
                        Pair(loadingState, browserDelegate)
                    }
                    .stateIn(lifecycleScope)
                    .collect { (loadingState, _) ->
                        if (loadingState != LoadingState.READY) return@collect
                        prepareWebLayer()
                    }
            }
        }

        lifecycleScope.launch {
            fetchNeevaUserInfo()
        }

        // Display the correct Fragment when the user switches profiles.
        lifecycleScope.launch {
            lifecycle.whenStarted {
                webModel.browserWrapperFlow.collect {
                    if (it.isIncognito) {
                        containerRegularProfile.visibility = View.GONE
                        containerIncognitoProfile.visibility = View.VISIBLE
                    } else {
                        containerRegularProfile.visibility = View.VISIBLE
                        containerIncognitoProfile.visibility = View.GONE
                    }
                }
            }
        }

        if (savedInstanceState != null && webModel.currentBrowser.isFullscreen()) {
            // If the activity was recreated because the user entered a fullscreen video or website,
            // hide the system bars.
            onEnterFullscreen()
        }
    }

    suspend fun fetchNeevaUserInfo() {
        withContext(dispatchers.io) {
            NeevaUser.fetch(apolloClient)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == Intent.ACTION_VIEW) {
            if (Uri.parse(intent.dataString).scheme == "neeva") {
                neevaUserToken.extractAuthTokenFromIntent(intent)?.let {
                    neevaUserToken.setToken(it)
                    webModel.onAuthTokenUpdated()
                    appNavModel?.showBrowser()
                    webModel.currentBrowser.activeTabModel.reload()
                }
            } else {
                intent.data?.let {
                    webModel.currentBrowser.activeTabModel.loadUrl(it, newTab = true)
                }
            }

            appNavModel?.showBrowser()
        }
    }

    private fun prepareWebLayer() {
        if (isFinishing || isDestroyed) return

        val topControlPlaceholder = layoutInflater.inflate(R.layout.fake_top_controls, null)
        val bottomControlPlaceholder = layoutInflater.inflate(R.layout.fake_bottom_controls, null)
        webModel.onWebLayerReady(
            topControlPlaceholder,
            bottomControlPlaceholder
        ) { fragment, isIncognito ->
            // Note the commitNow() instead of commit(). We want the fragment to get attached to
            // activity synchronously, so we can use all the functionality immediately. Otherwise we'd
            // have to wait until the commit is executed.
            val transaction = supportFragmentManager.beginTransaction()

            if (isIncognito) {
                transaction.replace(R.id.weblayer_incognito, fragment, TAG_INCOGNITO_PROFILE)
            } else {
                transaction.replace(R.id.weblayer_regular, fragment, TAG_REGULAR_PROFILE)
            }

            transaction.commitNow()
        }
    }

    override fun onBackPressed() {
        val browserWrapper = webModel.currentBrowser

        when {
            browserWrapper.exitFullscreen() -> {
                return
            }

            onBackPressedDispatcher.hasEnabledCallbacks() -> {
                onBackPressedDispatcher.onBackPressed()
            }

            browserWrapper.activeTabModel.navigationInfoFlow.value.canGoBackward -> {
                browserWrapper.activeTabModel.goBack()
            }

            browserWrapper.closeActiveChildTab() -> {
                return
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
        appNavModel?.showBrowser()

        val intent = Intent(this, NeevaActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        startActivity(intent)
    }

    override fun showContextMenuForTab(contextMenuParams: ContextMenuParams, tab: Tab) {
        tab.takeUnless { it.isDestroyed }?.browser?.fragment?.view?.apply {
            // Need to use the NeevaActivity as the context because the WebLayer View doesn't have
            // access to the correct resources.
            setOnCreateContextMenuListener(
                ContextMenuCreator(
                    webModel.browserWrapperFlow.value,
                    contextMenuParams,
                    tab,
                    this@NeevaActivity
                )
            )

            showContextMenu()
        }
    }

    override fun getDisplaySize(): Rect {
        return WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds
    }

    override fun onBottomBarOffsetChanged(offset: Int) {
        // Move the real bar when WebLayer says that the fake one is moving.
        bottomControlOffset.value = offset.toFloat()
    }

    override fun onTopBarOffsetChanged(offset: Int) {
        // Move the real bar when WebLayer says that the fake one is moving.
        topControlOffset.value = offset.toFloat()
    }

    override fun detachIncognitoFragment() {
        val fragment = supportFragmentManager.findFragmentByTag(TAG_INCOGNITO_PROFILE) ?: return
        val transaction = supportFragmentManager.beginTransaction()
        transaction.remove(fragment)
        transaction.commitNow()
    }
}
