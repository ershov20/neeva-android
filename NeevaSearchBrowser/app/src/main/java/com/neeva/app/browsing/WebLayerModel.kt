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
        private const val KEY_PREVIOUS_TAB_GUIDS = "previousTabGuids"
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

    private class PerTabState(val faviconFetcher: FaviconFetcher, vararg callbacks: TabCallback) {
        val tabCallbacks: MutableList<TabCallback> = callbacks.toMutableList()
    }

    val selectedTabFlow = MutableStateFlow<Pair<Tab?, Tab?>>(Pair(null, null))

    private inner class FullscreenCallbackImpl : FullscreenCallback() {
        private var systemVisibilityToRestore = 0
        private var exitFullscreenRunnable: Runnable? = null

        override fun onEnterFullscreen(exitFullscreenRunnable: Runnable) {
            this.exitFullscreenRunnable = exitFullscreenRunnable
            browserCallbacks.get()?.onEnterFullscreen()?.let {
                systemVisibilityToRestore = it
            }
        }

        override fun onExitFullscreen() {
            this.exitFullscreenRunnable = null
            browserCallbacks.get()?.onExitFullscreen(systemVisibilityToRestore)
        }

        fun canExitFullscreen() = exitFullscreenRunnable != null
        fun exitFullscreen() = exitFullscreenRunnable?.run()
    }

    private var uriRequestForNewTab: Uri? = null
    private var lastSavedInstanceState: Bundle? = null

    private lateinit var profile: Profile
    private lateinit var browser: Browser

    private val tabListCallback = object : TabListCallback() {
        override fun onActiveTabChanged(activeTab: Tab?) {
            val previousTab = selectedTabFlow.value.second
            selectedTabFlow.tryEmit(Pair(previousTab, activeTab))
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

    private val navigationCallback = object : NavigationCallback() {
        override fun onNavigationStarted(navigation: Navigation) {
            if (navigation.isSameDocument) return

            val timestamp = Date()
            val visit = Visit(
                timestamp = timestamp,
                visitRootID = DateConverter.fromDate(timestamp)!!,
                visitType = 0
            )
            historyViewModel.insert(navigation.uri, visit = visit)
        }
    }

    private val browserRestoreCallback: BrowserRestoreCallback = object : BrowserRestoreCallback() {
        override fun onRestoreCompleted() {
            super.onRestoreCompleted()

            restorePreviousTabList(lastSavedInstanceState);
            if (browser.tabs.count() == 1
                && browser.activeTab == browser.tabs.first()
                && browser.activeTab?.navigationController?.navigationListCurrentIndex == -1
            ) {
                browser.activeTab?.navigationController?.navigate(Uri.parse(appURL))
            } else if (browser.tabs.isEmpty()) {
                createTabFor(Uri.parse(appURL))
            }
        }

        private fun restorePreviousTabList(savedInstanceState: Bundle?) {
            savedInstanceState ?: return

            val previousTabGuids = savedInstanceState.getStringArray(KEY_PREVIOUS_TAB_GUIDS) ?: return
            val currentTabMap: MutableMap<String, Tab> = HashMap()
            browser.tabs.forEach { currentTabMap[it.guid] = it }
            previousTabGuids.forEach {
                val tab = currentTabMap[it] ?: return
                onNewTabAdded(tab)
            }
        }
    }

    private val cookieChangedCallback: CookieChangedCallback = object : CookieChangedCallback() {
        override fun onCookieChanged(cookie: String, cause: Int) {
            saveLoginCookieFrom(cookie)
        }
    }

    private val browserControlsOffsetCallback: BrowserControlsOffsetCallback = object : BrowserControlsOffsetCallback() {
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

    private var fullscreenCallback = FullscreenCallbackImpl()
    private val tabToPerTabState: HashMap<Tab, PerTabState> = HashMap()

    fun onSaveInstanceState(outState: Bundle) {
        // Store the stack of previous tab GUIDs that are used to set the next active tab when a tab
        // closes. Also used to setup various callbacks again on restore.
        val previousTabGuids = orderedTabList.value?.map { it.id }?.toTypedArray()
        outState.putStringArray(KEY_PREVIOUS_TAB_GUIDS, previousTabGuids)
    }

    fun onWebLayerReady(
        fragment: Fragment,
        topControlsPlaceholder: View,
        bottomControlsPlaceholder: View,
        savedInstanceState: Bundle?
    ) {
        lastSavedInstanceState = savedInstanceState
        // Have WebLayer Shell retain the fragment instance to simulate the behavior of
        // external embedders (note that if this is changed, then WebLayer Shell should handle
        // rotations and resizes itself via its manifest, as otherwise the user loses all state
        // when the shell is rotated in the foreground).
        fragment.retainInstance = true
        browser = Browser.fromFragment(fragment)!!

        browserCallbacks.get()?.getDisplaySize()?.let { windowSize ->
            browser.setMinimumSurfaceSize(windowSize.x, windowSize.y)
        }

        profile = browser.profile

        profile.setTablessOpenUrlCallback(object : OpenUrlCallback() {
            override fun getBrowserForNewTab(): Browser {
                return browser
            }

            override fun onTabAdded(tab: Tab) {
                registerTabCallbacks(tab)
            }
        })

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

        browser.registerBrowserRestoreCallback(browserRestoreCallback)
        profile.cookieManager.getCookie(Uri.parse(appURL)) {
            it?.split("; ")?.forEach { cookie ->
                saveLoginCookieFrom(cookie)
            }
        }
        profile.cookieManager.addCookieChangedCallback(Uri.parse(appURL),
            loginCookie, cookieChangedCallback)

        browser.registerTabListCallback(tabListCallback)
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
        tab.navigationController.registerNavigationCallback(navigationCallback)

        val tabCallback: TabCallback = object : TabCallback() {
            override fun bringTabToFront() {
                tab.browser.setActiveTab(tab)
                browserCallbacks.get()?.bringToForeground()
            }

            override fun onTitleUpdated(title: String) {
                domainViewModel.insert(tab.currentDisplayUrl.toString(), title)
                historyViewModel.insert(url = tab.currentDisplayUrl!!, title = title)
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

        val errorPageCallback = object : ErrorPageCallback() {
            // TODO(dan.alcantara): I don't know if we should be overriding this.
            override fun onBackToSafety(): Boolean {
                tab.navigationController.goBack()
                return true
            }

            // TODO(dan.alcantara): Although this should be showing the default error page, it
            //                      doesn't work.
            override fun getErrorPage(navigation: Navigation): ErrorPage? = null
        }
        tab.setErrorPageCallback(errorPageCallback)

        val newTabCallback: NewTabCallback = object : NewTabCallback() {
            override fun onNewTab(newTab: Tab, @NewTabType type: Int) = registerNewTab(newTab, type)
        }
        tab.setNewTabCallback(newTabCallback)

        val faviconFetcher = tab.createFaviconFetcher(object : FaviconCallback() {
            override fun onFaviconChanged(favicon: Bitmap?) {
                val icon = favicon ?: return
                domainViewModel.updateFaviconFor(tab.currentDisplayUrl.toString(), icon.toFavicon())
                historyViewModel.insert(url = tab.currentDisplayUrl!!, favicon = icon.toFavicon())
            }
        })
        tabToPerTabState[tab] = PerTabState(faviconFetcher, tabCallback, crashTabCallback)
    }

    private fun unregisterBrowserAndTabCallbacks() {
        browser.unregisterTabListCallback(tabListCallback)
        browser.unregisterBrowserRestoreCallback(browserRestoreCallback)
        browser.unregisterBrowserControlsOffsetCallback(browserControlsOffsetCallback)
        tabToPerTabState.forEach { unregisterTabCallbacks(it.key) }
        tabToPerTabState.clear()
    }

    private fun unregisterTabCallbacks(tab: Tab) {
        // Do not unset FullscreenCallback here which is called from onDestroy, since
        // unsetting FullscreenCallback also exits fullscreen.
        tab.navigationController.unregisterNavigationCallback(navigationCallback)

        tabToPerTabState[tab]?.apply {
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

