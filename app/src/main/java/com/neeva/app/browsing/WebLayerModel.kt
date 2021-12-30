package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neeva.app.NeevaBrowser
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.NeevaConstants.loginCookie
import com.neeva.app.history.DomainViewModel
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.storage.DateConverter
import com.neeva.app.storage.Visit
import com.neeva.app.storage.toFavicon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.set

class WebLayerModel(
    private val domainViewModel: DomainViewModel,
    private val historyViewModel: HistoryViewModel
): ViewModel() {
    companion object {
        private const val DIRECTORY_TAB_SCREENSHOTS = "tab_screenshots"

        fun getTabScreenshotDirectory(): File {
            return File(NeevaBrowser.context.filesDir, DIRECTORY_TAB_SCREENSHOTS)
        }

        fun getTabScreenshotFileUri(id: String): Uri {
            return File(getTabScreenshotDirectory(), "tab_$id.jpg").toUri()
        }

        @Suppress("UNCHECKED_CAST")
        class WebLayerModelFactory(
            private val domainModel: DomainViewModel,
            private val historyViewModel: HistoryViewModel
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return WebLayerModel(domainModel, historyViewModel) as T
            }
        }
    }

    var browserCallbacks: WeakReference<BrowserCallbacks> = WeakReference(null)

    private val _selectedTabFlow = MutableStateFlow<Pair<Tab?, Tab?>>(Pair(null, null))
    val selectedTabFlow: StateFlow<Pair<Tab?, Tab?>> = _selectedTabFlow

    private var uriRequestForNewTab: Uri? = null

    private lateinit var browser: Browser

    private val tabListCallback = object : TabListCallback() {
        override fun onActiveTabChanged(activeTab: Tab?) {
            val previousTab = _selectedTabFlow.value.second
            _selectedTabFlow.tryEmit(Pair(previousTab, activeTab))
            tabList.updatedSelectedTab(activeTab?.guid)
        }

        override fun onTabRemoved(tab: Tab) {
            val newIndex = (tabList.indexOf(tab) - 1).coerceAtLeast(0)
            tabList.remove(tab)

            unregisterTabCallbacks(tab)

            // Don't switch tabs unless there isn't one currently selected.
            // TODO(dan.alcantara): If this is a child tab, switch back to the one that spawned it.
            if (browser.activeTab == null) {
                if (orderedTabList.value.isNotEmpty()) {
                    browser.setActiveTab(tabList.getTab(newIndex))
                } else {
                    createTabFor(Uri.parse(appURL))
                    browser.setActiveTab(tabList.getTab(newIndex))
                    browserCallbacks.get()?.bringToForeground()
                }
            }
        }

        override fun onTabAdded(tab: Tab) {
            onNewTabAdded(tab)

            uriRequestForNewTab?.let {
                selectTab(tab)
                tab.navigationController.navigate(it)
                uriRequestForNewTab = null
            }
        }

        override fun onWillDestroyBrowserAndAllTabs() {
            unregisterBrowserAndTabCallbacks()
        }
    }

    private val newTabCallback: NewTabCallback = object : NewTabCallback() {
        override fun onNewTab(newTab: Tab, @NewTabType type: Int) = registerNewTab(newTab, type)
    }

    private var isBrowserCallbacksInitialized = false
    private lateinit var tabListRestorer: BrowserRestoreCallbackImpl

    private val browserControlsOffsetCallback = object : BrowserControlsOffsetCallback() {
        override fun onBottomViewOffsetChanged(offset: Int) {
            browserCallbacks.get()?.onBottomBarOffsetChanged(offset)
        }

        override fun onTopViewOffsetChanged(offset: Int) {
            browserCallbacks.get()?.onTopBarOffsetChanged(offset)
        }
    }

    private var tabList = TabList()
    val orderedTabList: StateFlow<List<TabInfo>>
        get() = tabList.orderedTabList

    private var fullscreenCallback = FullscreenCallbackImpl(
        { browserCallbacks.get()?.onEnterFullscreen() },
        { flags -> browserCallbacks.get()?.onExitFullscreen(flags) }
    )

    private val tabToTabCallbacks: HashMap<Tab, TabCallbacks> = HashMap()

    fun onSaveInstanceState(outState: Bundle) {
        BrowserRestoreCallbackImpl.onSaveInstanceState(outState, orderedTabList.value.map { it.id })
    }

    /**
     * Prepares the WebLayerModel to interface with the Browser.  Note that this is triggered every
     * time the Activity is recreated, which includes when the screen is rotated.  This means that
     * you should guard against two different instances of the same observer and or callback from
     * being registered.
     */
    fun onWebLayerReady(
        fragment: Fragment,
        topControlsPlaceholder: View,
        bottomControlsPlaceholder: View,
        savedInstanceState: Bundle?
    ) {
        browser = Browser.fromFragment(fragment)!!

        browserCallbacks.get()?.getDisplaySize()?.let { windowSize ->
            browser.setMinimumSurfaceSize(windowSize.x, windowSize.y)
        }

        // There appears to be a bug in WebLayer that prevents the bottom bar from being rendered,
        // and also prevents Composables from being re-rendered when their state changes.  To get
        // around this, we pass in a fake view that is the same height as the real bottom toolbar
        // and listen for the scrolling offsets, which we then apply to the real bottom toolbar.
        // This is a valid use case according to the BrowserControlsOffsetCallback.
        browser.setBottomView(bottomControlsPlaceholder)
        bottomControlsPlaceholder.layoutParams.height =
            fragment.context?.resources?.getDimensionPixelSize(com.neeva.app.R.dimen.bottom_toolbar_height) ?: 0
        bottomControlsPlaceholder.requestLayout()

        browser.setTopView(topControlsPlaceholder)
        topControlsPlaceholder.layoutParams.height =
            fragment.context?.resources?.getDimensionPixelSize(com.neeva.app.R.dimen.top_toolbar_height) ?: 0
        topControlsPlaceholder.requestLayout()

        if (!isBrowserCallbacksInitialized) {
            isBrowserCallbacksInitialized = true

            if (!::tabListRestorer.isInitialized) {
                tabListRestorer = BrowserRestoreCallbackImpl(
                    savedInstanceState,
                    browser,
                    { createTabFor(Uri.parse(appURL)) },
                    this::onNewTabAdded
                )
            }

            browser.registerTabListCallback(tabListCallback)
            browser.registerBrowserControlsOffsetCallback(browserControlsOffsetCallback)
            browser.registerBrowserRestoreCallback(tabListRestorer)

            browser.profile.setTablessOpenUrlCallback(
                object : OpenUrlCallback() {
                    override fun getBrowserForNewTab() = browser
                    override fun onTabAdded(tab: Tab) = registerTabCallbacks(tab)
                }
            )

            browser.profile.cookieManager.apply {
                getCookie(Uri.parse(appURL)) {
                    it?.split("; ")?.forEach { cookie ->
                        saveLoginCookieFrom(cookie)
                    }
                }

                addCookieChangedCallback(
                    Uri.parse(appURL),
                    loginCookie,
                    object : CookieChangedCallback() {
                        override fun onCookieChanged(cookie: String, cause: Int) {
                            saveLoginCookieFrom(cookie)
                        }
                    }
                )
            }
        }
    }

    fun createTabFor(uri: Uri) {
        uriRequestForNewTab = uri
        browser.createTab()
    }

    private fun onNewTabAdded(tab: Tab) {
        tabList.add(tab)
        registerTabCallbacks(tab)
    }

    fun registerNewTab(tab: Tab, @NewTabType type: Int) {
        registerTabCallbacks(tab)
        if (type == NewTabType.FOREGROUND_TAB) {
            selectTab(tab)
        }
    }

    fun registerTabCallbacks(tab: Tab) {
        // Avoid double-registering callbacks.  A telltale sign of this happening is the context
        // menu rapidly appearing and then disappearing, which happens when two ContextMenuCallback
        // instances have been registered and they both try to show the context menu.
        if (tabToTabCallbacks[tab] != null) return
        tabToTabCallbacks[tab] = TabCallbacks(tab)
    }

    private fun unregisterBrowserAndTabCallbacks() {
        if (isBrowserCallbacksInitialized) {
            browser.unregisterTabListCallback(tabListCallback)
            browser.unregisterBrowserControlsOffsetCallback(browserControlsOffsetCallback)
            browser.unregisterBrowserRestoreCallback(tabListRestorer)
            isBrowserCallbacksInitialized = false
        }

        // Avoid a ConcurrentModificationException by iterating on a copy of the keys rather than
        // the map itself.
        tabToTabCallbacks.keys.toList().forEach { unregisterTabCallbacks(it) }
        tabToTabCallbacks.clear()
    }

    private fun unregisterTabCallbacks(tab: Tab) {
        tabToTabCallbacks[tab]?.let {
            it.unregisterCallbacks()
            tabToTabCallbacks.remove(tab)
        }
    }

    fun canExitFullscreen() = fullscreenCallback.canExitFullscreen()
    fun exitFullscreen() = fullscreenCallback.exitFullscreen()

    fun onGridShown() = browser.activeTab?.captureAndSaveScreenshot(tabList)

    fun select(primitive: TabInfo) = tabList.findTab(primitive.id)?.let { selectTab(it) }
    fun close(primitive: TabInfo) = tabList.findTab(primitive.id)?.dispatchBeforeUnloadAndClose()

    fun selectTab(tab: Tab) {
        // Screenshot the previous tab right before it is replaced to keep it as fresh as possible.
        browser.activeTab?.takeUnless { it.isDestroyed }?.captureAndSaveScreenshot(tabList)

        browser.setActiveTab(tab)
    }

    /**
     * Encapsulates all callbacks related to a particular Tab's operation.
     *
     * Consumers must call [unregisterCallbacks] when the Tab's callbacks are no longer necessary.
     */
    private inner class TabCallbacks(private val tab: Tab) {
        /**
         * Triggered whenever a new Favicon is available for the given tab.  These are persisted
         * into the databases and provided to whatever UI needs them.
         */
        private val faviconFetcher = tab.createFaviconFetcher(object : FaviconCallback() {
            override fun onFaviconChanged(favicon: Bitmap?) {
                val icon = favicon ?: return

                domainViewModel.updateFaviconFor(
                    tab.currentDisplayUrl.toString(),
                    icon.toFavicon()
                )

                tab.currentDisplayUrl?.let { currentUrl ->
                    historyViewModel.insert(
                        url = currentUrl,
                        title = tab.currentDisplayTitle,
                        favicon = icon.toFavicon()
                    )
                }
            }
        })

        /**
         * Triggered whenever a navigation occurs in the Tab.  Navigations do not occur when [Tab]
         * are restored at startup, unless the Tab is the active tab in the [Browser].
         */
        val navigationCallback = object : NavigationCallback() {
            var visitToCommit: Visit? = null

            override fun onNavigationStarted(navigation: Navigation) {
                // TODO(dan.alcantara): Why is visitType set to 0 here?
                val timestamp = Date()
                visitToCommit = Visit(
                    timestamp = timestamp,
                    visitRootID = DateConverter.fromDate(timestamp)!!,
                    visitType = 0
                )
            }

            override fun onNavigationCompleted(navigation: Navigation) = commitVisit(navigation)
            override fun onNavigationFailed(navigation: Navigation) = commitVisit(navigation)

            private fun commitVisit(navigation: Navigation) {
                // Try to avoid recording visits to history when we are revisiting the same page.
                val shouldRecordVisit = when {
                    navigation.isSameDocument -> false
                    navigation.isReload -> false
                    navigation.isErrorPage -> false
                    navigation.isDownload -> false
                    else -> true
                }

                if (shouldRecordVisit) {
                    visitToCommit?.let { visit ->
                        historyViewModel.insert(
                            url = navigation.uri,
                            title = tab.currentDisplayTitle,
                            visit = visit
                        )

                        visitToCommit = null
                    }
                }
            }
        }

        /** General callbacks for the tab that allow it to interface with our app. */
        val tabCallback = object : TabCallback() {
            override fun bringTabToFront() {
                tab.browser.setActiveTab(tab)
                browserCallbacks.get()?.bringToForeground()
            }

            override fun onTitleUpdated(title: String) {
                domainViewModel.insert(tab.currentDisplayUrl.toString(), title)

                tab.currentDisplayUrl?.let {
                    historyViewModel.insert(url = it, title = title)
                }

                tabList.updateTabTitle(tab.guid, tab.currentDisplayTitle)
            }

            override fun onVisibleUriChanged(uri: Uri) {
                tabList.updateUrl(tab.guid, uri)
            }

            override fun showContextMenu(params: ContextMenuParams) {
                if (tab != browser.activeTab) return
                browserCallbacks.get()?.showContextMenuForTab(params, tab)
            }
        }

        /** Handles when the Tab crashes. */
        val crashTabCallback = CrashTabCallback(tab)

        init {
            tab.fullscreenCallback = fullscreenCallback
            tab.setErrorPageCallback(ErrorCallbackImpl(tab))
            tab.setNewTabCallback(newTabCallback)
            tab.navigationController.registerNavigationCallback(navigationCallback)
            tab.registerTabCallback(tabCallback)
            tab.registerTabCallback(crashTabCallback)
        }

        fun unregisterCallbacks() {
            faviconFetcher.destroy()

            // Do not unset FullscreenCallback here which is called from onDestroy, since
            // unsetting FullscreenCallback also exits fullscreen.
            // TODO(dan.alcantara): Carried this forward, but I don't get why this is a problem yet.

            tab.setErrorPageCallback(null)
            tab.setNewTabCallback(null)
            tab.navigationController.unregisterNavigationCallback(navigationCallback)
            tab.unregisterTabCallback(tabCallback)
            tab.unregisterTabCallback(crashTabCallback)
        }
    }
}

