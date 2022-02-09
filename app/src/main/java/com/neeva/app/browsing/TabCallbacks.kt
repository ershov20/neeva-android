package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.Dispatchers
import com.neeva.app.history.HistoryManager
import com.neeva.app.storage.TabScreenshotManager
import com.neeva.app.storage.entities.Visit
import com.neeva.app.storage.favicons.FaviconCache
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.FaviconCallback
import org.chromium.weblayer.FullscreenCallback
import org.chromium.weblayer.Navigation
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NewTabCallback
import org.chromium.weblayer.NewTabType
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback

/**
 * Encapsulates all callbacks related to a particular Tab's operation.
 *
 * Consumers must call [unregisterCallbacks] when the Tab's callbacks are no longer necessary.
 */
class TabCallbacks(
    private val isIncognito: Boolean,
    private val tab: Tab,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val historyManager: HistoryManager?,
    private val faviconCache: FaviconCache?,
    private val tabList: TabList,
    private val activityCallbackProvider: () -> ActivityCallbacks?,
    private val registerNewTab: (tab: Tab, type: Int) -> Unit,
    fullscreenCallback: FullscreenCallback,
    private val tabScreenshotManager: TabScreenshotManager
) {
    private val browser: Browser get() = tab.browser

    /**
     * Triggered whenever a new Favicon is available for the given tab.  These are persisted
     * into the databases and provided to whatever UI needs them.
     */
    private val faviconFetcher = tab.createFaviconFetcher(object : FaviconCallback() {
        override fun onFaviconChanged(favicon: Bitmap?) {
            if (faviconCache == null || favicon == null) {
                return
            }

            val title = tab.currentDisplayTitle
            val url = tab.currentDisplayUrl

            coroutineScope.launch {
                val faviconData = faviconCache.saveFavicon(url, favicon)

                if (!isIncognito) {
                    url?.let {
                        historyManager?.upsert(
                            url = it,
                            title = title,
                            favicon = faviconData
                        )
                    }
                }
            }
        }
    })

    private val newTabCallback: NewTabCallback = object : NewTabCallback() {
        override fun onNewTab(newTab: Tab, @NewTabType type: Int) {
            tabList.updateParentInfo(
                tab = newTab,
                parentTabId = tab.guid,
                tabOpenType = TabInfo.TabOpenType.CHILD_TAB
            )
            registerNewTab(newTab, type)
        }
    }

    /**
     * Triggered whenever a navigation occurs in the Tab.  Navigations do not occur when [Tab]
     * are restored at startup, unless the Tab is the active tab in the [Browser].
     */
    private val navigationCallback = object : NavigationCallback() {
        var visitToCommit: Visit? = null

        override fun onNavigationStarted(navigation: Navigation) {
            // We can only check if the browser is restoring state when the navigation starts.  Once
            // we hit the commit phase, it'll return false.
            val isRestoringState = tab.getBrowserIfAlive()?.isRestoringPreviousState == true
            if (isRestoringState) return

            visitToCommit = Visit(timestamp = Date())
        }

        override fun onNavigationCompleted(navigation: Navigation) = commitVisit(navigation)
        override fun onNavigationFailed(navigation: Navigation) = commitVisit(navigation)

        private fun commitVisit(navigation: Navigation) {
            if (tab.getBrowserIfAlive()?.activeTab == tab) {
                tabScreenshotManager.captureAndSaveScreenshot(tab)
            }

            // Try to avoid recording visits to history when we are revisiting the same page.
            val shouldRecordVisit = when {
                visitToCommit == null -> false
                isIncognito -> false
                navigation.isSameDocument -> false
                navigation.isReload -> false
                navigation.isErrorPage -> false
                navigation.isDownload -> false
                else -> true
            }

            if (shouldRecordVisit) {
                visitToCommit?.let { visit ->
                    val uri = navigation.uri
                    val title = tab.currentDisplayTitle

                    coroutineScope.launch {
                        historyManager?.upsert(
                            url = uri,
                            title = title,
                            visit = visit
                        )
                    }
                }
                visitToCommit = null
            }
        }
    }

    /** General callbacks for the tab that allow it to interface with our app. */
    private val tabCallback = object : TabCallback() {
        override fun bringTabToFront() {
            browser.setActiveTab(tab)
            activityCallbackProvider()?.bringToForeground()
        }

        override fun onTitleUpdated(title: String) {
            tab.currentDisplayUrl?.let {
                coroutineScope.launch {
                    historyManager?.upsert(url = it, title = title)
                }
            }

            tabList.updateTabTitle(tab.guid, title)
        }

        override fun onVisibleUriChanged(uri: Uri) {
            tabList.updateUrl(tab.guid, uri)
        }

        override fun showContextMenu(params: ContextMenuParams) {
            if (tab != browser.activeTab) return
            activityCallbackProvider()?.showContextMenuForTab(params, tab)
        }
    }

    /** Handles when the Tab crashes. */
    private val crashTabCallback = CrashTabCallback(tab)

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
