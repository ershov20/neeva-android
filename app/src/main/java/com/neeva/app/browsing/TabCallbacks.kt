// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.TabInfo.TabOpenType
import com.neeva.app.contentfilter.ContentFilterCallbacks
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.contentfilter.ContentFilteringPreferences
import com.neeva.app.contentfilter.ScriptInjectionManager
import com.neeva.app.contentfilter.TabContentFilterModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Visit
import com.neeva.app.storage.favicons.FaviconCache
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.ContentFilterCallback
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.FaviconCallback
import org.chromium.weblayer.FullscreenCallback
import org.chromium.weblayer.Navigation
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NewTabCallback
import org.chromium.weblayer.NewTabType
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback
import timber.log.Timber

/**
 * Encapsulates all callbacks related to a particular Tab's operation.
 *
 * Consumers must call [unregisterCallbacks] when the Tab's callbacks are no longer necessary.
 */
class TabCallbacks(
    browserFlow: StateFlow<Browser?>,
    private val isIncognito: Boolean,
    internal val tab: Tab,
    private val coroutineScope: CoroutineScope,
    private val historyManager: HistoryManager?,
    private val faviconCache: FaviconCache?,
    private val tabList: TabList,
    private val activityCallbackProvider: ActivityCallbackProvider,
    private val registerNewTab: (tab: Tab, type: Int) -> Unit,
    fullscreenCallback: FullscreenCallback,
    private val contentFilterModel: ContentFilterModel,
    domainProvider: DomainProvider,
    private val scriptInjectionManager: ScriptInjectionManager,
    private val clientLogger: ClientLogger?,
    private val neevaConstants: NeevaConstants
) {
    val tabContentFilterModel = TabContentFilterModel(
        browserFlow = browserFlow,
        tabId = tab.guid,
        trackingDataFlow = contentFilterModel.trackingDataFlow,
        enableCookieNoticeSuppression = contentFilterModel.enableTrackingProtection,
        cookieNoticeBlockedFlow = contentFilterModel.cookieNoticeBlockedFlow,
        easyListRuleBlockedFlow = contentFilterModel.easyListRuleBlockedFlow,
        domainProvider = domainProvider,
        trackersAllowList = contentFilterModel.trackersAllowList
    )

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
                parentSpaceId = null,
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
            // Prevent WebLayer from trying to fire an Intent out if we detect that the user is
            // being sent to our backend for login authentication.  This is necessary because the
            // backend will send a `neeva:` URI that gets Intented out.
            if (FirstRunModel.isNeevaLoginUri(navigation.uri, neevaConstants)) {
                navigation.disableIntentProcessing()
            }

            // reset cookie cutter
            contentFilterCallback.onContentFilterStatsUpdated()
            tabContentFilterModel.cookieNoticeBlocked = false

            tabList.updateIsCrashed(tab.guid, isCrashed = false)

            // We can only check if the browser is restoring state when the navigation starts.  Once
            // we hit the commit phase, it'll return false.
            val isRestoringState = browserFlow.value?.isRestoringPreviousState == true
            if (!isRestoringState) {
                visitToCommit = Visit(timestamp = Date())
            }
        }

        override fun onNavigationCompleted(navigation: Navigation) {
            // Make sure that we have a new js context before injecting,
            // and that navigation isn't just from push/replaceState
            // also make sure we're a scheme worth injecting into
            val isHttp = navigation.uri.scheme == "http" || navigation.uri.scheme == "https"
            if (!navigation.isSameDocument && isHttp) {
                scriptInjectionManager.injectNavigationCompletedScripts(
                    navigation.uri,
                    tab,
                    tabContentFilterModel
                )
            }

            commitVisit(navigation)
        }

        override fun onNavigationFailed(navigation: Navigation) {
            // https://github.com/neevaco/neeva-android/issues/582
            // Navigation can fail if the user is still being asked if they want to open another app
            // to view the website.
            if (navigation.isUserDecidingIntentLaunch) {
                Timber.w("Navigation failed because user is deciding intent launch")
                return
            }

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
            // Try to avoid recording visits to history when we are revisiting the same page.
            val shouldRecordVisit = when {
                navigation.uri.scheme == "about" -> false
                visitToCommit == null -> false
                isIncognito -> false
                navigation.isSameDocument -> false
                navigation.isReload -> false
                navigation.isErrorPage -> false
                navigation.isDownload -> false
                else -> true
            }

            if (shouldRecordVisit) {
                clientLogger?.logNavigation(navigation.uri)

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

    /** Prunes recorded SAYT queries associated with navigation entries that no longer exist. */
    private val searchNavigationCallback = object : NavigationCallback() {
        override fun onNavigationCompleted(navigation: Navigation) = pruneQueryList()
        override fun onNavigationFailed(navigation: Navigation) = pruneQueryList()

        private fun pruneQueryList() {
            tab.takeUnless { it.isDestroyed }?.let {
                tabList.pruneQueryNavigations(tab.guid, tab.navigationController)
            }
        }
    }

    /** General callbacks for the tab that allow it to interface with our app. */
    private val tabCallback = object : TabCallback() {
        override fun bringTabToFront() {
            browserFlow.setActiveTab(tab.guid)
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
            contentFilterCallback.onContentFilterStatsUpdated()
            tabList.updateUrl(tab.guid, uri)
        }

        override fun showContextMenu(params: ContextMenuParams) {
            if (tab.isDestroyed) {
                Timber.e("Cannot display context menu: Tab is destroyed")
                return
            }

            if (browserFlow.getActiveTabId() != tab.guid) {
                Timber.e("Cannot display context menu: Tab is not active")
                return
            }

            if (browserFlow.getActiveTab() != tab) {
                Timber.w("Warning: Tab instances are not equal.  Showing context menu anyway")
            }

            activityCallbackProvider.get()?.showContextMenuForTab(params, tab)
        }

        override fun onRenderProcessGone() {
            if (!tab.isDestroyed && !tab.willAutomaticallyReloadAfterCrash()) {
                tabList.updateIsCrashed(tab.guid, isCrashed = true)
            }
        }
    }

    private val contentFilterCallback = object : ContentFilterCallback() {
        override fun onContentFilterStatsUpdated() {
            tabContentFilterModel.updateStats(tab.contentFilterStats)
            tabContentFilterModel.easyListRuleBlocked =
                tab.getContentFilterStats("easylist").isNotEmpty()
        }
    }

    private val cookieCutterCallbacks = object : ContentFilterCallbacks() {
        override fun onGetPreferences(): ContentFilteringPreferences {
            return ContentFilteringPreferences.fromSet(
                contentFilterModel.cookieCuttingPreferences.value
            )
        }

        override fun onNoticeHandled() {
            tabContentFilterModel.cookieNoticeBlocked = true
        }

        override fun onIsFlagged(origin: String): Boolean {
            // TODO: introduce site flagging to prevent reload loops
            return false
        }

        override fun onLogProvider(providerId: String) {
            // TODO: Log when a provider is matched
        }
    }

    init {
        tab.fullscreenCallback = fullscreenCallback
        tab.setErrorPageCallback(ErrorCallbackImpl(activityCallbackProvider))
        tab.setNewTabCallback(newTabCallback)
        tab.navigationController.registerNavigationCallback(navigationCallback)
        tab.navigationController.registerNavigationCallback(searchNavigationCallback)
        tab.registerTabCallback(tabCallback)
        tab.setContentFilterCallback(contentFilterCallback)
        scriptInjectionManager.initializeMessagePassing(tab, cookieCutterCallbacks)
    }

    fun unregisterCallbacks() {
        faviconFetcher.destroy()

        // Do not unset FullscreenCallback here which is called from onDestroy, since
        // unsetting FullscreenCallback also exits fullscreen.
        // TODO(dan.alcantara): Carried this forward, but I don't get why this is a problem yet.

        tab.setErrorPageCallback(null)
        tab.setNewTabCallback(null)
        tab.navigationController.unregisterNavigationCallback(navigationCallback)
        tab.navigationController.unregisterNavigationCallback(searchNavigationCallback)
        tab.setContentFilterCallback(null)
        tab.unregisterTabCallback(tabCallback)
        scriptInjectionManager.unregisterMessagePassing(tab)
    }
}
