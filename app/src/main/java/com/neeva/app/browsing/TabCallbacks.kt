package com.neeva.app.browsing

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.history.HistoryManager
import com.neeva.app.storage.FaviconCache
import com.neeva.app.storage.TypeConverters
import com.neeva.app.storage.Visit
import java.lang.ref.WeakReference
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    appContext: Application,
    private val coroutineScope: CoroutineScope,
    private val historyManager: HistoryManager?,
    private val faviconCache: FaviconCache?,
    private val tabList: TabList,
    private val activityCallbacks: WeakReference<ActivityCallbacks>,
    private val registerNewTab: (tab: Tab, type: Int) -> Unit,
    fullscreenCallback: FullscreenCallback
) {
    private val browser: Browser get() = tab.browser

    /**
     * Triggered whenever a new Favicon is available for the given tab.  These are persisted
     * into the databases and provided to whatever UI needs them.
     */
    private val faviconFetcher = tab.createFaviconFetcher(object : FaviconCallback() {
        override fun onFaviconChanged(favicon: Bitmap?) {
            if (isIncognito || historyManager == null || faviconCache == null || favicon == null) {
                return
            }

            val url = tab.currentDisplayUrl

            coroutineScope.launch {
                val faviconData = withContext(Dispatchers.IO) {
                    faviconCache.saveFavicon(favicon)
                }

                url?.let {
                    historyManager.insert(
                        coroutineScope = coroutineScope,
                        url = it,
                        title = tab.currentDisplayTitle,
                        favicon = faviconData
                    )
                }

                historyManager.updateDomainFavicon(
                    url = tab.currentDisplayUrl.toString(),
                    favicon = faviconData
                )
            }
        }
    })

    private val newTabCallback: NewTabCallback = object : NewTabCallback() {
        override fun onNewTab(newTab: Tab, @NewTabType type: Int) {
            tabList.updateParentTabId(newTab.guid, parentTabId = tab.guid)
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
            // TODO(dan.alcantara): Why is visitType set to 0 here?
            val timestamp = Date()
            visitToCommit = Visit(
                timestamp = timestamp,
                visitRootID = TypeConverters.fromDate(timestamp)!!,
                visitType = 0
            )
        }

        override fun onNavigationCompleted(navigation: Navigation) = commitVisit(navigation)
        override fun onNavigationFailed(navigation: Navigation) = commitVisit(navigation)

        private fun commitVisit(navigation: Navigation) {
            // Try to avoid recording visits to history when we are revisiting the same page.
            val shouldRecordVisit = when {
                isIncognito -> false
                navigation.isSameDocument -> false
                navigation.isReload -> false
                navigation.isErrorPage -> false
                navigation.isDownload -> false
                else -> true
            }

            if (shouldRecordVisit) {
                visitToCommit?.let { visit ->
                    historyManager?.insert(
                        coroutineScope = coroutineScope,
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
    private val tabCallback = object : TabCallback() {
        override fun bringTabToFront() {
            browser.setActiveTab(tab)
            activityCallbacks.get()?.bringToForeground()
        }

        override fun onTitleUpdated(title: String) {
            tab.currentDisplayUrl?.let {
                historyManager?.insert(coroutineScope = coroutineScope, url = it, title = title)
            }

            tabList.updateTabTitle(tab.guid, title)
        }

        override fun onVisibleUriChanged(uri: Uri) {
            tabList.updateUrl(tab.guid, uri)
        }

        override fun showContextMenu(params: ContextMenuParams) {
            if (tab != browser.activeTab) return
            activityCallbacks.get()?.showContextMenuForTab(params, tab)
        }
    }

    /** Handles when the Tab crashes. */
    private val crashTabCallback = CrashTabCallback(tab, appContext)

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
