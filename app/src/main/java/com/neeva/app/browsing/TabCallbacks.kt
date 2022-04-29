package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.Dispatchers
import com.neeva.app.browsing.TabInfo.TabOpenType
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
    private val activityCallbackProvider: ActivityCallbackProvider,
    private val registerNewTab: (tab: Tab, type: Int) -> Unit,
    fullscreenCallback: FullscreenCallback,
    private val tabScreenshotManager: TabScreenshotManager
) {
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
                tabOpenType = TabOpenType.CHILD_TAB
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
            tabList.updateIsCrashed(tab.guid, isCrashed = false)

            // We can only check if the browser is restoring state when the navigation starts.  Once
            // we hit the commit phase, it'll return false.
            val isRestoringState = tab.getBrowserIfAlive()?.isRestoringPreviousState == true
            if (!isRestoringState) {
                visitToCommit = Visit(timestamp = Date())
            }
        }

        override fun onNavigationCompleted(navigation: Navigation) = commitVisit(navigation)

        override fun onNavigationFailed(navigation: Navigation) {
            when {
                navigation.isKnownProtocol -> {
                    // If protocol is known (e.g. https://, http://) and the navigation failed,
                    // treat it like a normal navigation failure.
                    commitVisit(navigation)
                }

                !navigation.wasIntentLaunched() -> {
                    // Workaround for:
                    // * https://github.com/neevaco/neeva-android/issues/232
                    // * https://github.com/neevaco/neeva-android/issues/526
                    //
                    // WebLayer doesn't seem to know how to handle external Intent firing when we
                    // use it.  Until we have a better idea of why it can't fire an Intent out, fire
                    // the Intent out ourselves in case Android can handle it.
                    //
                    // If firing an Intent out fails, WebLayer will sometimes run through a list of
                    // other URIs until one works, or use a fallback URL that is sent as part of the
                    // URI.  Clicking on a link via a TikTok webpage, for example, will sometimes
                    // fire out three Intent URIs in succession to try to load their app.  If those
                    // fail, then an Intent to the Play Store page for the TikTok app is fired.
                    val navigationListSize = tab.navigationController.navigationListSize
                    val tabOpenType = tabList.getTabInfo(tab.guid)?.data?.openType

                    // Check if a new tab was created just for the navigation.
                    val newTabWasCreatedForFailedNavigation = when {
                        navigationListSize == 0 -> true
                        navigationListSize == 1 && tabOpenType == TabOpenType.VIA_INTENT -> true
                        else -> false
                    }

                    activityCallbackProvider.get()?.fireExternalIntentForUri(
                        navigation.uri,
                        newTabWasCreatedForFailedNavigation
                    )
                }
            }
        }

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
            tab.getBrowserIfAlive()?.setActiveTab(tab)
            activityCallbackProvider.get()?.bringToForeground()
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
            if (tab != tab.getBrowserIfAlive()?.activeTab) return
            activityCallbackProvider.get()?.showContextMenuForTab(params, tab)
        }

        override fun onRenderProcessGone() {
            if (!tab.isDestroyed && !tab.willAutomaticallyReloadAfterCrash()) {
                tabList.updateIsCrashed(tab.guid, isCrashed = true)
            }
        }
    }

    init {
        tab.fullscreenCallback = fullscreenCallback
        tab.setErrorPageCallback(ErrorCallbackImpl(activityCallbackProvider))
        tab.setNewTabCallback(newTabCallback)
        tab.navigationController.registerNavigationCallback(navigationCallback)
        tab.registerTabCallback(tabCallback)
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
    }
}
