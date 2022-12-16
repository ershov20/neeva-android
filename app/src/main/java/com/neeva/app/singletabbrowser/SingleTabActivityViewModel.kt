// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.singletabbrowser

import android.app.Application
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.window.layout.WindowMetrics
import com.neeva.app.R
import com.neeva.app.browsing.WebLayerFactory
import com.neeva.app.browsing.currentDisplayTitle
import com.neeva.app.browsing.currentDisplayUrl
import com.neeva.app.browsing.takeIfAlive
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NewTabCallback
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback
import org.chromium.weblayer.TabListCallback
import org.chromium.weblayer.WebLayer

@HiltViewModel
class SingleTabActivityViewModel @Inject constructor(
    application: Application,
    private val webLayerFactory: WebLayerFactory,
) : AndroidViewModel(application) {
    companion object {
        private const val FRAGMENT_TAG = "SINGLE_TAB_FRAGMENT_TAG"
        private const val SINGLE_TAB_PROFILE = "SINGLE_TAB_PROFILE"
    }

    private lateinit var _browser: Browser
    private val browser: Browser? get() = _browser.takeIfAlive()

    private val isWebLayerLoaded = CompletableDeferred(false)
    private val tabList = mutableListOf<String>()

    /** IDs of Tabs that have had callbacks registered. */
    private val registeredTabIds = mutableMapOf<String, TabCallbacks>()

    val titleFlow = MutableStateFlow<String?>(null)
    val urlFlow = MutableStateFlow<String?>(null)
    val progressFlow = MutableStateFlow(100)

    suspend fun initializeWebLayer(
        supportFragmentManager: FragmentManager,
        windowMetrics: WindowMetrics,
        callback: (success: Boolean) -> Unit
    ) {
        // Let WebLayer finish loading.
        suspendCoroutine { continuation ->
            webLayerFactory.load {
                continuation.resume(Unit)
            }
        }

        initializeBrowser(supportFragmentManager, windowMetrics)

        browser?.registerTabListCallback(tabListCallback)
            ?: run {
                callback(false)
                return
            }

        isWebLayerLoaded.complete(true)
        callback(true)
    }

    private fun attachWebLayerBrowserFragment(supportFragmentManager: FragmentManager): Fragment {
        val transaction = supportFragmentManager.beginTransaction()

        // If the Activity died in the background, it's possible for the Fragment to stick
        // around and be recreated.
        var fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.also {
            if (it.isDetached) transaction.attach(it)
        }

        // Create a new Fragment to interface with WebLayer.
        if (fragment == null) {
            fragment = WebLayer.createBrowserFragment(SINGLE_TAB_PROFILE)
                .also {
                    transaction.add(R.id.weblayer_fragment_view_container, it, FRAGMENT_TAG)
                }
        }

        transaction.commitNow()

        // Keep the WebLayer instance across Activity restarts so that the Browser doesn't get
        // deleted when the configuration changes (e.g. the screen is rotated in fullscreen).
        @Suppress("DEPRECATION")
        fragment.retainInstance = true

        return fragment
    }

    /**
     * Because initialization is asynchronous and the user can exit at any point, we have to keep
     * checking if the Browser (and each of its Tabs) are alive right before we try to use them.
     */
    private suspend fun initializeBrowser(
        supportFragmentManager: FragmentManager,
        windowMetrics: WindowMetrics
    ) {
        // Attach the fragment to the Activity.
        val fragment: Fragment = attachWebLayerBrowserFragment(supportFragmentManager)

        _browser = Browser.fromFragment(fragment) ?: throw IllegalStateException()
        windowMetrics.bounds.apply {
            _browser.setMinimumSurfaceSize(width(), height())
        }

        // Wait for WebLayer to finish restoring its state.
        suspendCoroutine { continuation ->
            val restoreCallback = object : BrowserRestoreCallback() {
                override fun onRestoreCompleted() {
                    browser?.unregisterBrowserRestoreCallback(this)
                    continuation.resume(Unit)
                }
            }

            browser?.let { liveBrowser ->
                // WebLayer's Browser initialization can be finicky: If the [Browser] was
                // already fully restored when we added the callback, then our callback doesn't
                // fire.  This can happen if the app dies in the background, with WebLayer's
                // Fragments automatically creating the Browser before we have a chance to hook
                // into it.  We work around this by manually calling onRestoreCompleted() if
                // it's already done.
                liveBrowser.registerBrowserRestoreCallback(restoreCallback)

                if (!liveBrowser.isRestoringPreviousState) {
                    continuation.resume(Unit)
                }
            } ?: run {
                continuation.resume(Unit)
            }
        }
    }

    suspend fun onBackPressed(): Boolean {
        isWebLayerLoaded.await()

        return withContext(Dispatchers.Main) {
            val liveBrowser = browser ?: return@withContext false

            liveBrowser.activeTab
                ?.takeUnless { it.isDestroyed }
                ?.let { activeTab ->
                    if (activeTab.navigationController.canGoBack()) {
                        activeTab.navigationController.goBack()
                        return@withContext true
                    }

                    if (liveBrowser.tabs.size > 1) {
                        // A tab was created via a popup. Close it so that we can send the user back
                        // to the previously active tab.
                        activeTab.dispatchBeforeUnloadAndClose()
                        return@withContext true
                    }
                }

            return@withContext false
        }
    }

    suspend fun loadUrl(url: Uri): Boolean = withContext(Dispatchers.Main) {
        // Make sure that we've actually finished initialization before continuing.
        isWebLayerLoaded.await()

        return@withContext browser?.let { liveBrowser ->
            // Close any existing tabs.
            liveBrowser.tabs.forEach { it.dispatchBeforeUnloadAndClose() }

            // Create a new tab to perform the load.
            val tab = liveBrowser.createTab()
            liveBrowser.setActiveTab(tab)
            tab.navigationController.navigate(url)
            true
        } ?: false
    }

    inner class TabCallbacks(private val tab: Tab) {
        /** Monitors when the page's URL or title changes. */
        private val tabCallback = object : TabCallback() {
            override fun onTitleUpdated(title: String) = updateToolbar(tab)
            override fun onVisibleUriChanged(uri: Uri) = updateToolbar(tab)
        }

        /** Monitors page loading progress. */
        private val navigationCallback = object : NavigationCallback() {
            override fun onLoadProgressChanged(progress: Double) {
                if (isTabActive(tab)) {
                    updateProgressBar((progress * 100.0).roundToInt())
                }
            }

            override fun onLoadStateChanged(isLoading: Boolean, shouldShowLoadingUi: Boolean) {
                if (isTabActive(tab)) {
                    if (!isLoading || !shouldShowLoadingUi) {
                        updateProgressBar(100)
                    }
                }
            }
        }

        /**
         * Set a [NewTabCallback] so that WebLayer will allow popups (even if we don't do anything
         * inside of the callback). We don't actually seem to need to do anything here because it'll
         * still call TabListCallback with the new tab.
         */
        private val newTabCallback = object : NewTabCallback() {
            override fun onNewTab(tab: Tab, type: Int) {}
        }

        init {
            if (!tab.isDestroyed) {
                tab.setNewTabCallback(newTabCallback)
                tab.registerTabCallback(tabCallback)
                tab.navigationController.registerNavigationCallback(navigationCallback)
            }
        }

        fun unregisterCallbacks() {
            if (tab.isDestroyed) return

            tab.setNewTabCallback(null)
            tab.unregisterTabCallback(tabCallback)
            tab.navigationController.unregisterNavigationCallback(navigationCallback)
        }
    }

    private val tabListCallback = object : TabListCallback() {
        override fun onWillDestroyBrowserAndAllTabs() {
            browser?.unregisterTabListCallback(this)
        }

        override fun onTabAdded(tab: Tab) {
            tabList.add(tab.guid)
            registeredTabIds[tab.guid] = TabCallbacks(tab)

            val liveBrowser = browser ?: return
            if (!liveBrowser.isRestoringPreviousState) {
                liveBrowser.setActiveTab(tab)
            }
        }

        override fun onTabRemoved(tab: Tab) {
            tabList.remove(tab.guid)
            registeredTabIds[tab.guid]?.unregisterCallbacks()

            // Select a new active tab.
            val liveBrowser = browser ?: return
            if (liveBrowser.activeTab == null) {
                tabList.asReversed().forEach { guid ->
                    liveBrowser.tabs
                        .firstOrNull { it.guid == guid && !it.isDestroyed }
                        ?.let {
                            liveBrowser.setActiveTab(it)
                            return
                        }
                }
            }
        }

        override fun onActiveTabChanged(activeTab: Tab?) {
            if (activeTab?.isDestroyed == false) {
                updateToolbar(activeTab)
                updateProgressBar(100)
            }
        }
    }

    private fun updateToolbar(tab: Tab) {
        if (isTabActive(tab)) {
            titleFlow.value = tab.currentDisplayTitle
            urlFlow.value = tab.currentDisplayUrl?.toString()
        }
    }

    private fun updateProgressBar(progress: Int) {
        progressFlow.value = progress
    }

    private fun isTabActive(tab: Tab?): Boolean {
        return tab?.isDestroyed == false && tab == browser?.activeTab
    }
}
