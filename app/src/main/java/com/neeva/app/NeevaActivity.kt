package com.neeva.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.browsing.ActivityCallbacks
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.ContextMenuCreator
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.firstrun.FirstRun
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsModel
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.NeevaUser
import com.neeva.app.storage.SpaceStore
import com.neeva.app.ui.theme.NeevaTheme
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    @Inject lateinit var domainProviderImpl: DomainProviderImpl
    @Inject lateinit var historyDatabase: HistoryDatabase
    @Inject lateinit var historyManager: HistoryManager

    private val webModel by viewModels<WebLayerModel>()

    private val settingsModel by viewModels<SettingsModel>()

    private val appNavModel by viewModels<AppNavModel> {
        AppNavModel.AppNavModelFactory(spaceStore)
    }

    /**
     * View provided to WebLayer that allows us to receive information about when the bottom toolbar
     * needs to be scrolled off.  We provide a placeholder instead of the real view because WebLayer
     * appears to have a bug that prevents the bottom view from rendering correctly.
     * TODO(dan.alcantara): Revisit this once we move past WebLayer/Chromium v96.
     */
    private lateinit var bottomControls: View
    private lateinit var browserUI: View

    private lateinit var containerRegularProfile: View
    private lateinit var containerIncognitoProfile: View

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webModel.activityCallbacks = WeakReference(this)

        setContentView(R.layout.main)
        browserUI = findViewById<ComposeView>(R.id.browser_ui).apply {
            setContent {
                NeevaTheme {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        val browserWrapper: BrowserWrapper
                            by webModel.browserWrapperFlow.collectAsState()

                        BrowserUI(
                            browserWrapper.urlBarModel,
                            browserWrapper.suggestionsModel,
                            browserWrapper.activeTabModel,
                            browserWrapper.faviconCache
                        )
                    }
                }
            }
        }
        browserUI.background = null

        findViewById<ComposeView>(R.id.app_nav).setContent {
            NeevaTheme {
                Surface(color = Color.Transparent) {
                    val browserWrapper: BrowserWrapper
                        by webModel.browserWrapperFlow.collectAsState()
                    AppNav(appNavModel, webModel, settingsModel) { space ->
                        lifecycleScope.launch {
                            browserWrapper.activeTabModel.modifySpace(space, apolloClient)
                            appNavModel.showBrowser()
                        }
                    }
                }
            }
        }

        bottomControls = findViewById<ComposeView>(R.id.tab_toolbar).apply {
            setContent {
                NeevaTheme {
                    val browserWrapper: BrowserWrapper
                        by webModel.browserWrapperFlow.collectAsState()
                    val isEditing: Boolean by
                    browserWrapper.urlBarModel.isEditing.collectAsState(false)

                    Surface(color = MaterialTheme.colorScheme.background) {
                        if (!isEditing) {
                            TabToolbar(
                                TabToolbarModel(
                                    appNavModel::showNeevaMenu,
                                    appNavModel::showAddToSpace
                                ) {
                                    browserWrapper.takeScreenshotOfActiveTab {
                                        appNavModel.showCardGrid()
                                    }
                                },
                                browserWrapper.activeTabModel
                            )
                        }
                    }
                }
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
                    .collect { (loadingState, browserDelegate) ->
                        if (loadingState != LoadingState.READY) return@collect
                        prepareWebLayer()
                    }
            }
        }

        lifecycleScope.launchWhenCreated {
            NeevaUser.fetch(apolloClient)
        }

        if (FirstRun.shouldShowFirstRun(this)) {
            appNavModel.showFirstRun()
            FirstRun.firstRunDone(this)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            if (Uri.parse(intent.dataString).scheme == "neeva") {
                User.extractAuthTokenFromIntent(intent)?.let {
                    User.setToken(this, it)
                    webModel.onAuthTokenUpdated()
                    appNavModel.showBrowser()
                }
            } else {
                intent.data?.let {
                    webModel.currentBrowser.activeTabModel.loadUrl(it, newTab = true)
                }
            }
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
                containerRegularProfile.visibility = View.GONE
                containerIncognitoProfile.visibility = View.VISIBLE
                transaction.replace(R.id.weblayer_incognito, fragment, TAG_INCOGNITO_PROFILE)
            } else {
                containerIncognitoProfile.visibility = View.GONE
                containerRegularProfile.visibility = View.VISIBLE
                transaction.replace(R.id.weblayer_regular, fragment, TAG_REGULAR_PROFILE)
            }

            transaction.commitNow()
        }
    }

    override fun onBackPressed() {
        val browserWrapper = webModel.browserWrapperFlow.value

        when {
            browserWrapper.canExitFullscreen() -> {
                browserWrapper.exitFullscreen()
            }

            !appNavModel.isCurrentState(AppNavState.BROWSER) -> {
                appNavModel.showBrowser()
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

    override fun onEnterFullscreen(): Int {
        // This avoids an extra resize.
        val attrs: WindowManager.LayoutParams = window.attributes
        attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        window.attributes = attrs
        val decorView: View = window.decorView

        // Caching the system ui visibility is ok for shell, but likely not ok for real code.
        val systemVisibilityToRestore = decorView.systemUiVisibility
        decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        return systemVisibilityToRestore
    }

    override fun onExitFullscreen(systemVisibilityToRestore: Int) {
        val decorView: View = window.decorView
        decorView.systemUiVisibility = systemVisibilityToRestore
        val attrs: WindowManager.LayoutParams = window.attributes
        if (attrs.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS != 0) {
            attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
            window.attributes = attrs
        }
    }

    override fun bringToForeground() {
        appNavModel.showBrowser()

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

    override fun getDisplaySize(): Point {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getRealSize(point)
        return point
    }

    override fun onBottomBarOffsetChanged(offset: Int) {
        // Move the real bar when WebLayer says that the fake one is moving.
        bottomControls.translationY = offset.toFloat()
    }

    override fun onTopBarOffsetChanged(offset: Int) {
        // Move the real bar when WebLayer says that the fake one is moving.
        browserUI.translationY = offset.toFloat()
    }

    override fun onDeleteIncognitoProfile() {
        val fragment = supportFragmentManager.findFragmentByTag(TAG_INCOGNITO_PROFILE) ?: return
        val transaction = supportFragmentManager.beginTransaction()
        transaction.remove(fragment)
        transaction.commitNow()

        containerRegularProfile.visibility = View.VISIBLE
        containerIncognitoProfile.visibility = View.GONE
    }
}
