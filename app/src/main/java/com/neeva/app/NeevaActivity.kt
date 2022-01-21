package com.neeva.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.compose.rememberNavController
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.browsing.ActivityCallbacks
import com.neeva.app.browsing.ContextMenuCreator
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.settings.SettingsModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.NeevaUser
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
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
    @Inject lateinit var settingsModel: SettingsModel
    @Inject lateinit var webModel: WebLayerModel

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

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webModel.activityCallbacks = WeakReference(this)

        setContentView(R.layout.main)

        findViewById<ComposeView>(R.id.browser_ui).apply {
            setContent {
                val navController = rememberNavController()

                appNavModel = remember(navController) {
                    AppNavModel(
                        navController = navController,
                        webLayerModel = webModel,
                        coroutineScope = lifecycleScope
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
                    apolloClient = apolloClient
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

        lifecycleScope.launchWhenCreated {
            NeevaUser.fetch(apolloClient)
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
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            if (Uri.parse(intent.dataString).scheme == "neeva") {
                User.extractAuthTokenFromIntent(intent)?.let {
                    User.setToken(this, it)
                    webModel.onAuthTokenUpdated()
                    appNavModel?.showBrowser()
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

    override fun onEnterFullscreen(onBackPressedCallback: OnBackPressedCallback): Int {
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

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

    override fun getDisplaySize(): Point {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getRealSize(point)
        return point
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
