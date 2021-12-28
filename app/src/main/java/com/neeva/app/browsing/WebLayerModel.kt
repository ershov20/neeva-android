package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neeva.app.NeevaBrowser
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.NeevaConstants.loginCookie
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.storage.DateConverter
import com.neeva.app.storage.DomainViewModel
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

    private class PerTabState(
        val faviconFetcher: FaviconFetcher,
        val navigationCallback: NavigationCallback,
        vararg callbacks: TabCallback
    ) {
        val tabCallbacks: MutableList<TabCallback> = callbacks.toMutableList()
    }

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
                if (orderedTabList.value?.isNotEmpty() == true) {
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

    private var tabListRestorer: BrowserRestoreCallbackImpl? = null

    private val browserControlsOffsetCallback = object : BrowserControlsOffsetCallback() {
        override fun onBottomViewOffsetChanged(offset: Int) {
            browserCallbacks.get()?.onBottomBarOffsetChanged(offset)
        }

        override fun onTopViewOffsetChanged(offset: Int) {
            browserCallbacks.get()?.onTopBarOffsetChanged(offset)
        }
    }

    private var tabList = TabList()
    val orderedTabList: LiveData<List<TabInfo>>
        get() = tabList.orderedTabList

    private var fullscreenCallback = FullscreenCallbackImpl(
        { browserCallbacks.get()?.onEnterFullscreen() },
        { flags -> browserCallbacks.get()?.onExitFullscreen(flags) }
    )

    private val tabToPerTabState: HashMap<Tab, PerTabState> = HashMap()

    fun onSaveInstanceState(outState: Bundle) {
        tabListRestorer?.onSaveInstanceState(outState, orderedTabList.value)
    }

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

        browser.registerBrowserControlsOffsetCallback(browserControlsOffsetCallback)
        browser.registerTabListCallback(tabListCallback)

        tabListRestorer = BrowserRestoreCallbackImpl(
            savedInstanceState,
            browser,
            { createTabFor(Uri.parse(appURL)) },
            this::onNewTabAdded
        ).also {
            browser.registerBrowserRestoreCallback(it)
        }

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
        tab.fullscreenCallback = fullscreenCallback
        tab.setErrorPageCallback(ErrorCallbackImpl(tab))
        tab.setNewTabCallback(newTabCallback)

        val navigationCallback = object : NavigationCallback() {
            var visitToCommit: Visit? = null

            override fun onNavigationStarted(navigation: Navigation) {
                if (navigation.isSameDocument) return

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

            fun commitVisit(navigation: Navigation) {
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
        tab.navigationController.registerNavigationCallback(navigationCallback)

        val tabCallback: TabCallback = object : TabCallback() {
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
        tab.registerTabCallback(tabCallback)

        val crashTabCallback = CrashTabCallback(tab)
        tab.registerTabCallback(crashTabCallback)

        val faviconFetcher = tab.createFaviconFetcher(object : FaviconCallback() {
            override fun onFaviconChanged(favicon: Bitmap?) {
                val icon = favicon ?: return

                domainViewModel.updateFaviconFor(tab.currentDisplayUrl.toString(), icon.toFavicon())

                tab.currentDisplayUrl?.let { currentUrl ->
                    historyViewModel.insert(
                        url = currentUrl,
                        title = tab.currentDisplayTitle,
                        favicon = icon.toFavicon()
                    )
                }
            }
        })
        tabToPerTabState[tab] = PerTabState(
            faviconFetcher,
            navigationCallback,
            tabCallback,
            crashTabCallback
        )
    }

    private fun unregisterBrowserAndTabCallbacks() {
        browser.unregisterTabListCallback(tabListCallback)
        tabListRestorer?.let { browser.unregisterBrowserRestoreCallback(it) }
        browser.unregisterBrowserControlsOffsetCallback(browserControlsOffsetCallback)

        tabToPerTabState.forEach { unregisterTabCallbacks(it.key) }
        tabToPerTabState.clear()
    }

    private fun unregisterTabCallbacks(tab: Tab) {
        // Do not unset FullscreenCallback here which is called from onDestroy, since
        // unsetting FullscreenCallback also exits fullscreen.
        tabToPerTabState[tab]?.apply {
            tab.navigationController.unregisterNavigationCallback(navigationCallback)

            tabCallbacks.forEach(tab::unregisterTabCallback)
            faviconFetcher.destroy()
        }
        tabToPerTabState.remove(tab)

        tab.setErrorPageCallback(null)
        tab.setNewTabCallback(null)
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
}

