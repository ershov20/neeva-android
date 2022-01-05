package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.NeevaBrowser
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.NeevaConstants.loginCookie
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.publicsuffixlist.SuffixListManager
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.storage.DateConverter
import com.neeva.app.storage.Visit
import com.neeva.app.storage.toFavicon
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.urlbar.URLBarModel
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.set

/**
 * Manages and maintains the interface between the Neeva browser and WebLayer.
 *
 * The WebLayer [Browser] maintains a set of [Tab]s that we are supposed to keep track of.  These
 * classes must be monitored using various callbacks that fire whenever a new tab is opened, or
 * whenever the current tab changes (e.g.).
 */
class WebLayerModel(
    suffixListManager: SuffixListManager,
    private val historyViewModel: HistoryViewModel,
    apolloClient: ApolloClient
): ViewModel() {
    companion object {
        private const val DIRECTORY_TAB_SCREENSHOTS = "tab_screenshots"

        fun getTabScreenshotDirectory(): File {
            return File(NeevaBrowser.context.filesDir, DIRECTORY_TAB_SCREENSHOTS)
        }

        fun getTabScreenshotFileUri(id: String): Uri {
            return File(getTabScreenshotDirectory(), "tab_$id.jpg").toUri()
        }
    }

    var browserCallbacks: WeakReference<BrowserCallbacks> = WeakReference(null)

    data class CreateNewTabInfo(
        val uri: Uri,
        val parentTabId: String? = null
    )
    /**
     * Keeps track of data that must be applied to the next tab that is created.  This is necessary
     * because the [Browser] doesn't allow you to create a tab and navigate to somewhere in the same
     * call.  Rather, the [Browser] creates the tab asynchronously and we are expected to detect
     * when it is opened.
     */
    private var newTabInfo: CreateNewTabInfo? = null

    private lateinit var browser: Browser

    private val tabListCallback = object : TabListCallback() {
        override fun onActiveTabChanged(activeTab: Tab?) {
            activeTabModel.onActiveTabChanged(activeTab)
            tabList.updatedSelectedTab(activeTab?.guid)
        }

        override fun onTabRemoved(tab: Tab) {
            val newIndex = (tabList.indexOf(tab) - 1).coerceAtLeast(0)
            val tabInfo = tabList.remove(tab)
            val parentTab = tabInfo?.parentTabId?.let { tabList.findTab(it) }

            unregisterTabCallbacks(tab)

            // If the tab that was removed wasn't the active tab, don't switch tabs.
            if (browser.activeTab != null) return

            when {
                parentTab != null -> {
                    browser.setActiveTab(parentTab)
                }

                orderedTabList.value.isNotEmpty() -> {
                    browser.setActiveTab(tabList.getTab(newIndex))
                }

                else -> {
                    createTabWithUri(Uri.parse(appURL), parentTabId = null)
                    browser.setActiveTab(tabList.getTab(newIndex))
                    browserCallbacks.get()?.bringToForeground()
                }
            }
        }

        override fun onTabAdded(tab: Tab) {
            onNewTabAdded(tab)

            newTabInfo?.let {
                selectTab(tab)
                tabList.updateParentTabId(tab.guid, parentTabId = it.parentTabId)
                tab.navigationController.navigate(it.uri)
                newTabInfo = null
            }
        }

        override fun onWillDestroyBrowserAndAllTabs() {
            unregisterBrowserAndTabCallbacks()
        }
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

    private val tabCallbackMap: HashMap<Tab, TabCallbacks> = HashMap()

    val activeTabModel = ActiveTabModel(this::createTabWithUri)

    val urlBarModel = URLBarModel(viewModelScope, activeTabModel) {
        // Pull new suggestions from the database according to what's currently in the URL bar.
        historyViewModel.updateSuggestionQuery(it)
    }

    val suggestionsModel = SuggestionsModel(
        viewModelScope,
        historyViewModel,
        urlBarModel,
        apolloClient
    )

    init {
        suffixListManager.initialize(viewModelScope)
    }

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
                    { createTabWithUri(Uri.parse(appURL), parentTabId = null) },
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

    /**
     * Creates a new tab and shows the given [uri].
     *
     * Because [Browser] only allows you to create a Tab without a URL, we save the URL for when
     * the [TabListCallback] tells us the new tab has been added.
     */
    fun createTabWithUri(uri: Uri, parentTabId: String?) {
        newTabInfo = CreateNewTabInfo(uri, parentTabId)
        browser.createTab()
    }

    private fun onNewTabAdded(tab: Tab) {
        tabList.add(tab)
        registerTabCallbacks(tab)
    }

    /**
     * Registers all the callbacks that are necessary for the Tab when it is opened.
     *
     * @param tab Tab that was just created.  It will be added to the [TabList] via another callback.
     * @param type Type of tab being opened.  Most cases result in foregrounding the tab.
     */
    fun registerNewTab(tab: Tab, @NewTabType type: Int) {
        registerTabCallbacks(tab)

        when (type) {
            NewTabType.FOREGROUND_TAB,
            NewTabType.NEW_POPUP,
            NewTabType.NEW_WINDOW -> {
                selectTab(tab)
            }

            else -> { /* Do nothing. */ }
        }
    }

    /**
     * Takes a newly created [tab] and registers all the callbacks we need to keep track of and
     * manipulate its state.  Calling this function when the tab's callbacks were already registered
     * results in a no-op.
     */
    fun registerTabCallbacks(tab: Tab) {
        if (tabCallbackMap[tab] != null) return
        tabCallbackMap[tab] = TabCallbacks(tab)
    }

    /** Removes all the callbacks that are set up to interact with WebLayer. */
    private fun unregisterBrowserAndTabCallbacks() {
        if (isBrowserCallbacksInitialized) {
            browser.unregisterTabListCallback(tabListCallback)
            browser.unregisterBrowserControlsOffsetCallback(browserControlsOffsetCallback)
            browser.unregisterBrowserRestoreCallback(tabListRestorer)
            isBrowserCallbacksInitialized = false
        }

        // Avoid a ConcurrentModificationException by iterating on a copy of the keys rather than
        // the map itself.
        tabCallbackMap.keys.toList().forEach { unregisterTabCallbacks(it) }
        tabCallbackMap.clear()
    }

    private fun unregisterTabCallbacks(tab: Tab) {
        tabCallbackMap[tab]?.let {
            it.unregisterCallbacks()
            tabCallbackMap.remove(tab)
        }
    }

    fun canExitFullscreen() = fullscreenCallback.canExitFullscreen()
    fun exitFullscreen() = fullscreenCallback.exitFullscreen()

    fun onGridShown() = browser.activeTab?.captureAndSaveScreenshot(tabList)

    fun selectTab(primitive: TabInfo) = tabList.findTab(primitive.id)?.let { selectTab(it) }
    fun closeTab(primitive: TabInfo) = tabList.findTab(primitive.id)?.dispatchBeforeUnloadAndClose()

    fun selectTab(tab: Tab) {
        // Screenshot the previous tab right before it is replaced to keep it as fresh as possible.
        browser.activeTab?.takeUnless { it.isDestroyed }?.captureAndSaveScreenshot(tabList)

        browser.setActiveTab(tab)
    }

    /**
     * Closes the active Tab if it is a child of another Tab.  No-op if the active tab is not a
     * child of another Tab.
     *
     * @return True if the tab was the child of an existing Tab.
     */
    fun closeActiveChildTab(): Boolean {
        return browser.activeTab
            ?.let { activeTab ->
                val tabInfo = tabList.getTabInfo(activeTab.guid)
                tabInfo?.parentTabId?.let {
                    closeTab(tabInfo)
                    true
                }
            } ?: false
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

                historyViewModel.updateFaviconFor(
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
                tab.currentDisplayUrl?.let {
                    historyViewModel.insert(url = it, title = title)
                }

                tabList.updateTabTitle(tab.guid, title)
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

    @Suppress("UNCHECKED_CAST")
    class WebLayerModelFactory(
        private val suffixListManager: SuffixListManager,
        private val historyViewModel: HistoryViewModel,
        private val apolloClient: ApolloClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return WebLayerModel(suffixListManager, historyViewModel, apolloClient) as T
        }
    }
}

