package com.neeva.app.browsing

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.history.HistoryManager
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.urlbar.URLBarModelImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserControlsOffsetCallback
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.NewTabType
import org.chromium.weblayer.OpenUrlCallback
import org.chromium.weblayer.PageInfoDisplayOptions
import org.chromium.weblayer.Profile
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabListCallback
import org.chromium.weblayer.UrlBarController

/**
 * Encapsulates everything that is needed to interact with a WebLayer [Browser].
 *
 * [Browser] represents everything required to interact with a set of tabs that are associated with
 * a particular [org.chromium.weblayer.Profile].  Subclasses must be careful to ensure that anything
 * done is allowed by the incognito state defined by the Profile, which means that we are explicitly
 * trying to avoid recording history or automatically firing queries & mutations via Apollo (e.g.).
 */
abstract class BrowserWrapper internal constructor(
    val isIncognito: Boolean,
    protected val appContext: Context,
    protected val coroutineScope: CoroutineScope,
    protected val dispatchers: Dispatchers,
    protected val activityCallbackProvider: () -> ActivityCallbacks?,
    val suggestionsModel: SuggestionsModel?,
    val faviconCache: FaviconCache,
    protected val spaceStore: SpaceStore?,
    private val _activeTabModel: ActiveTabModelImpl,
    private val _urlBarModel: URLBarModelImpl,
    private val _findInPageModel: FindInPageModelImpl
) : FaviconCache.ProfileProvider {
    constructor(
        isIncognito: Boolean,
        appContext: Context,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        activityCallbackProvider: () -> ActivityCallbacks?,
        suggestionsModel: SuggestionsModel?,
        faviconCache: FaviconCache,
        spaceStore: SpaceStore?
    ) : this(
        isIncognito = isIncognito,
        appContext = appContext,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        activityCallbackProvider = activityCallbackProvider,
        suggestionsModel = suggestionsModel,
        faviconCache = faviconCache,
        spaceStore = spaceStore,
        _activeTabModel = ActiveTabModelImpl(
            spaceStore = spaceStore,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers
        ),
        _urlBarModel = URLBarModelImpl(
            suggestionFlow = suggestionsModel?.autocompleteSuggestionFlow ?: MutableStateFlow(null),
            appContext = appContext,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            faviconCache = faviconCache
        ),
        _findInPageModel = FindInPageModelImpl()
    )

    private val tabList = TabList()
    private val tabCallbackMap: HashMap<Tab, TabCallbacks> = HashMap()

    val orderedTabList: StateFlow<List<TabInfo>> get() = tabList.orderedTabList

    /** Tracks if the active tab needs to be reloaded due to a renderer crash. */
    val shouldDisplayCrashedTab: Flow<Boolean> =
        tabList.orderedTabList.map {
            it.any { tabInfo -> tabInfo.isSelected && tabInfo.isCrashed }
        }

    private val browserInitializationLock = Object()

    private lateinit var fragment: Fragment

    /**
     * Updated whenever the [Browser] is recreated.
     * If you don't need to monitor changes, you can directly access the [browser] field.
     */
    private val browserFlow = MutableStateFlow<Browser?>(null)

    protected val browser: Browser?
        get() = browserFlow.value?.takeUnless { it.isDestroyed }

    val activeTabModel: ActiveTabModel get() = _activeTabModel
    val findInPageModel: FindInPageModel get() = _findInPageModel
    val urlBarModel: URLBarModel get() = _urlBarModel

    /** Tracks whether the user needs to be kept in the CardGrid if they're on that screen. */
    val userMustStayInCardGridFlow: StateFlow<Boolean>

    val tabScreenshotManager: TabScreenshotManager by lazy { createTabScreenshotManager() }

    abstract val historyManager: HistoryManager?

    private var tabListRestorer: BrowserRestoreCallback? = null

    val urlBarControllerFlow: Flow<UrlBarController?> = browserFlow.map { it?.urlBarController }

    private val _isLazyTabFlow = MutableStateFlow(false)
    val isLazyTabFlow: StateFlow<Boolean> = _isLazyTabFlow

    /** Tracks when the WebLayer [Browser] has finished restoration and the [tabList] is ready. */
    private val isBrowserRestored = CompletableDeferred<Boolean>()

    init {
        faviconCache.profileProvider = this

        userMustStayInCardGridFlow = orderedTabList
            .combine(_isLazyTabFlow) { tabs, isLazyTab -> tabs.isEmpty() && !isLazyTab }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

        coroutineScope.launch {
            urlBarModel.isEditing.collectLatest { isEditing ->
                _isLazyTabFlow.value = _isLazyTabFlow.value && isEditing
            }
        }
    }

    private var fullscreenCallback = FullscreenCallbackImpl(
        activityEnterFullscreen = { activityCallbackProvider()?.onEnterFullscreen() },
        activityExitFullscreen = { activityCallbackProvider()?.onExitFullscreen() }
    )

    private val tabListCallback = object : TabListCallback() {
        override fun onActiveTabChanged(activeTab: Tab?) {
            fullscreenCallback.exitFullscreen()
            _activeTabModel.onActiveTabChanged(activeTab)
            tabList.updatedSelectedTab(activeTab?.guid)
        }

        override fun onTabRemoved(tab: Tab) {
            // Delete any screenshot that was taken for the tab.
            val tabId = tab.guid
            coroutineScope.launch(dispatchers.io) {
                tabScreenshotManager.deleteScreenshot(tabId)
            }

            // Remove the tab from our local state.
            val newIndex = (tabList.indexOf(tab) - 1).coerceAtLeast(0)
            val tabInfo = tabList.remove(tab)

            // Remove all the callbacks associated with the tab to avoid any callbacks after the tab
            // gets destroyed.
            unregisterTabCallbacks(tab)

            // If a tab is still active, this tab was closed in the background.
            browser?.let {
                if (it.activeTab != null) return

                val parentTab = tabInfo?.data?.parentTabId?.let {
                    parentId ->
                    tabList.findTab(parentId)
                }
                when {
                    parentTab != null -> it.setActiveTab(parentTab)
                    orderedTabList.value.isNotEmpty() -> it.setActiveTab(tabList.getTab(newIndex))
                }
            }
        }

        override fun onTabAdded(tab: Tab) {
            onNewTabAdded(tab)
            activityCallbackProvider()?.resetToolbarOffset()
        }

        override fun onWillDestroyBrowserAndAllTabs() {
            unregisterBrowserAndTabCallbacks()
        }
    }

    private fun cleanCacheDirectory() {
        coroutineScope.launch(dispatchers.io) {
            // Clean up any unused tab thumbnails.
            val liveTabGuids = tabList.orderedTabList.value.map { it.id }
            tabScreenshotManager.cleanCacheDirectory(liveTabGuids)

            // Clean up any unused favicons.
            historyManager?.getAllFaviconUris()?.let { faviconCache.pruneCacheDirectory(it) }
        }
    }

    private val browserControlsOffsetCallback = object : BrowserControlsOffsetCallback() {
        override fun onBottomViewOffsetChanged(offset: Int) {
            activityCallbackProvider()?.onBottomBarOffsetChanged(offset)
        }

        override fun onTopViewOffsetChanged(offset: Int) {
            activityCallbackProvider()?.onTopBarOffsetChanged(offset)
        }
    }

    /**
     * Creates a Fragment that contains the [Browser] used to interface with WebLayer.
     *
     * This [Browser] that is created must be associated with the correct incognito or non-incognito
     * profile to avoid leaking state.
     */
    abstract fun createBrowserFragment(): Fragment

    /** Creates a [TabScreenshotManager] that can be used to persist preview images of tabs. */
    abstract fun createTabScreenshotManager(): TabScreenshotManager

    /** Returns the [Browser] from the given [fragment]. */
    internal open fun getBrowserFromFragment(fragment: Fragment): Browser? {
        return Browser.fromFragment(fragment)
    }

    /** Prepares the WebLayer Browser to interface with our app. */
    fun createAndAttachBrowser(
        topControlsPlaceholder: View,
        bottomControlsPlaceholder: View,
        displaySize: Rect,
        fragmentAttacher: (fragment: Fragment, isIncognito: Boolean) -> Unit
    ) = synchronized(browserInitializationLock) {
        if (!::fragment.isInitialized) {
            // TODO(https://github.com/neevaco/neeva-android/issues/318): We should try to reuse any
            // existing Fragments that were kept by the FragmentManager after the Activity died in
            // the background.
            fragment = createBrowserFragment()

            // Keep the WebLayer instance across Activity restarts so that the Browser doesn't get
            // deleted when the configuration changes (e.g. the screen is rotated in fullscreen).
            fragment.retainInstance = true
        }

        fragmentAttacher(fragment, isIncognito)
        if (browserFlow.value == null) {
            browserFlow.value = getBrowserFromFragment(fragment)
        }

        val browser = browserFlow.value ?: throw IllegalStateException()
        registerBrowserCallbacks(browser)

        browser.setMinimumSurfaceSize(displaySize.width(), displaySize.height())

        // There appears to be a bug in WebLayer that prevents the bottom bar from being rendered,
        // and also prevents Composables from being re-rendered when their state changes.  To get
        // around this, we pass in a fake view that is the same height as the real bottom toolbar
        // and listen for the scrolling offsets, which we then apply to the real bottom toolbar.
        // This is a valid use case according to the BrowserControlsOffsetCallback.
        val resources = appContext.resources
        browser.setBottomView(bottomControlsPlaceholder)
        bottomControlsPlaceholder.layoutParams.height =
            resources?.getDimensionPixelSize(com.neeva.app.R.dimen.bottom_toolbar_height) ?: 0
        bottomControlsPlaceholder.requestLayout()

        browser.setTopView(topControlsPlaceholder)
        topControlsPlaceholder.layoutParams.height =
            resources?.getDimensionPixelSize(com.neeva.app.R.dimen.top_toolbar_height) ?: 0
        topControlsPlaceholder.requestLayout()
    }

    /**
     * WebLayer automatically creates an empty tab in some situations (e.g. browser profile
     * creation).  Override this to do something with the tab, like navigate somewhere else or
     * close the tab entirely.
     */
    protected abstract fun onBlankTabCreated(tab: Tab)

    @CallSuper
    protected open fun registerBrowserCallbacks(browser: Browser): Boolean {
        if (tabListRestorer != null) {
            // If the tabListRestorer is non-null, we've previously registered callbacks on the
            // Browser.  This happens because the WebLayer Fragment survives Activity recreation.
            // Bail early to avoid adding additional copies of observers and callbacks.
            return false
        }

        val restorer = BrowserRestoreCallbackImpl(
            tabList = tabList,
            browser = browser,
            cleanCache = this::cleanCacheDirectory,
            onBlankTabCreated = this::onBlankTabCreated,
            onEmptyTabList = {
                createTabWithUri(
                    uri = Uri.parse(NeevaConstants.appURL),
                    parentTabId = null,
                    isViaIntent = false
                )
            },
            afterRestoreCompleted = { isBrowserRestored.complete(true) }
        ).also {
            tabListRestorer = it
        }

        browser.registerTabListCallback(tabListCallback)
        browser.registerBrowserControlsOffsetCallback(browserControlsOffsetCallback)

        browser.profile.setTablessOpenUrlCallback(
            object : OpenUrlCallback() {
                override fun getBrowserForNewTab() = browser
                override fun onTabAdded(tab: Tab) = registerTabCallbacks(tab)
            }
        )

        // Let Neeva know that it's serving an Android client.
        browser.profile.cookieManager.setCookie(
            Uri.parse(NeevaConstants.appURL),
            NeevaConstants.browserTypeCookie.toString() +
                NeevaConstants.browserVersionCookie.toString(),
            null
        )

        browser.registerBrowserRestoreCallback(restorer)
        if (!browser.isRestoringPreviousState) {
            // WebLayer's Browser initialization can be finicky: If the [Browser] was already fully
            // restored when we added the callback, then our callback doesn't fire.  This can happen
            // if the app dies in the background, with WebLayer's Fragments automatically being
            // creating the Browser before we have a chance to hook into it.
            // We work around this by manually calling onRestoreCompleted() if it's already done.
            restorer.onRestoreCompleted()
            _activeTabModel.onActiveTabChanged(browser.activeTab)
        }

        return true
    }

    /**
     * Registers all the callbacks that are necessary for the Tab when it is opened.
     *
     * @param tab Tab that was just created.  It will be added to the [TabList] via another callback.
     * @param type Type of tab being opened.  Most cases result in foregrounding the tab.
     */
    private fun registerNewTab(tab: Tab, @NewTabType type: Int) {
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
        tabCallbackMap[tab] = TabCallbacks(
            isIncognito = isIncognito,
            tab = tab,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            historyManager = historyManager,
            faviconCache = faviconCache,
            tabList = tabList,
            activityCallbackProvider = activityCallbackProvider,
            registerNewTab = this::registerNewTab,
            fullscreenCallback = fullscreenCallback,
            tabScreenshotManager = tabScreenshotManager
        )
    }

    private fun unregisterTabCallbacks(tab: Tab) {
        tabCallbackMap[tab]?.let {
            it.unregisterCallbacks()
            tabCallbackMap.remove(tab)
        }
    }

    /** Removes all the callbacks that are set up to interact with WebLayer. */
    @CallSuper
    open fun unregisterBrowserAndTabCallbacks() {
        synchronized(browserInitializationLock) {
            browser?.apply {
                unregisterTabListCallback(tabListCallback)
                unregisterBrowserControlsOffsetCallback(browserControlsOffsetCallback)
                tabListRestorer?.let { unregisterBrowserRestoreCallback(it) }
            }

            // Avoid a ConcurrentModificationException by iterating on a copy of the keys rather than
            // the map itself.
            tabCallbackMap.keys.toList().forEach {
                unregisterTabCallbacks(it)
                tabList.remove(it)
            }
            tabCallbackMap.clear()
            tabList.clear()
            _activeTabModel.onActiveTabChanged(null)

            browserFlow.value = null
            tabListRestorer = null
        }
    }

    /** Creates a new tab and shows the given [uri]. */
    private fun createTabWithUri(uri: Uri, parentTabId: String?, isViaIntent: Boolean) {
        browser?.let {
            val tabOpenType = when {
                parentTabId != null -> TabInfo.TabOpenType.CHILD_TAB
                isViaIntent -> TabInfo.TabOpenType.VIA_INTENT
                else -> TabInfo.TabOpenType.DEFAULT
            }

            val newTab = it.createTab()
            newTab.navigationController.navigate(uri)

            // onTabAdded should have been called by this point, allowing us to store the extra
            // information about the Tab.
            tabList.updateParentInfo(
                tab = newTab,
                parentTabId = parentTabId,
                tabOpenType = tabOpenType
            )

            selectTab(newTab)
        }
    }

    private fun onNewTabAdded(tab: Tab) {
        tabList.add(tab)
        registerTabCallbacks(tab)
    }

    fun closeTab(primitive: TabInfo) = tabList.findTab(primitive.id)?.dispatchBeforeUnloadAndClose()

    fun closeAllTabs() = tabList.forEach { it.dispatchBeforeUnloadAndClose() }

    fun selectTab(primitive: TabInfo) = tabList.findTab(primitive.id)?.let { selectTab(it) }

    private fun selectTab(tab: Tab) {
        // Screenshot the previous tab right before it is replaced to keep it as fresh as possible.
        takeScreenshotOfActiveTab()

        browser?.setActiveTab(tab)
    }

    fun takeScreenshotOfActiveTab(onCompleted: () -> Unit = {}) {
        val tab = browser?.activeTab
        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
    }

    fun showPageInfo() = browserFlow.value?.urlBarController?.showPageInfo(
        PageInfoDisplayOptions.builder().build()
    )

    /**
     * Closes the active Tab if and only if it was opened via a VIEW Intent.
     * @return True if the tab was closed.
     */
    fun closeActiveTabIfOpenedViaIntent() =
        conditionallyCloseActiveTab(TabInfo.TabOpenType.VIA_INTENT)

    /**
     * Closes the active Tab if and only if it was opened as a child of another Tab.
     * @return True if the tab was closed.
     */
    fun closeActiveChildTab(): Boolean =
        conditionallyCloseActiveTab(TabInfo.TabOpenType.CHILD_TAB)

    private fun conditionallyCloseActiveTab(expected: TabInfo.TabOpenType): Boolean {
        return browser?.activeTab
            ?.let { activeTab ->
                val tabInfo = tabList.getTabInfo(activeTab.guid)
                tabInfo?.data?.openType
                    ?.takeIf { it == expected }
                    ?.let {
                        closeTab(tabInfo)
                        true
                    }
            } ?: false
    }

    /**
     * Allows the user to use the URL bar and see suggestions without opening a tab until they
     * trigger a navigation.
     */
    fun openLazyTab() {
        _isLazyTabFlow.value = true
        urlBarModel.onRequestFocus()
    }

    /** Returns true if the [Browser] is maintaining no tabs. */
    fun hasNoTabs(): Boolean = tabList.hasNoTabs()
    fun hasNoTabsFlow(): Flow<Boolean> = tabList.hasNoTabsFlow

    /** Returns true if the user should be forced to go to the card grid. */
    fun userMustBeShownCardGrid(): Boolean = tabList.hasNoTabs() && !_isLazyTabFlow.value

    fun isFullscreen(): Boolean = fullscreenCallback.isFullscreen()
    fun exitFullscreen(): Boolean = fullscreenCallback.exitFullscreen()

    /** Provides access to the WebLayer profile. */
    override fun getProfile(): Profile? = browser?.profile

    /** Returns a list of cookies split by key and values. */
    fun getCookiePairs(uri: Uri, callback: (List<CookiePair>) -> Unit) {
        browser?.profile?.cookieManager?.apply {
            getCookie(uri) { cookiesString ->
                val cookies = cookiesString
                    .split(";")
                    .map { cookie ->
                        val parsedCookie = cookie.trim().split("=")
                        CookiePair(parsedCookie.first(), parsedCookie.last())
                    }
                callback(cookies)
            }
        }
    }

    // region: Active tab operations
    fun goBack() = _activeTabModel.goBack()
    fun goForward() = _activeTabModel.goForward()
    fun reload() = _activeTabModel.reload()

    /**
     * Start a load of the given [uri].
     *
     * If the user is currently in the process of opening a new tab lazily, this will open a new Tab
     * with the URL.
     *
     * If the BrowserWrapper needs to redirect the user to another URI (e.g. if the user is
     * performing a search in Incognito for the first time), the load may be delayed by a network
     * call to get the updated URL.
     */
    fun loadUrl(
        uri: Uri,
        inNewTab: Boolean = _isLazyTabFlow.value,
        isViaIntent: Boolean = false,
        parentTabId: String? = null,
        onLoadStarted: () -> Unit = {}
    ) = coroutineScope.launch {
        // Wait until the Browser finishes restoration.  If you try to load a URL in a new tab
        // before restoration has completed, the Browser may drop the request on the floor.
        isBrowserRestored.await()

        // Check if the user needs to be redirected somewhere else.
        val urlToLoad = if (shouldInterceptLoad(uri)) {
            getReplacementUrl(uri)
        } else {
            uri
        }

        if (inNewTab || _activeTabModel.activeTab == null) {
            createTabWithUri(
                uri = urlToLoad,
                parentTabId = parentTabId,
                isViaIntent = isViaIntent
            )
        } else {
            _activeTabModel.loadUrlInActiveTab(urlToLoad)
        }

        onLoadStarted()
    }

    /** Checks whether or not the BrowserWrapper wants to block loading of the given [uri]. */
    open fun shouldInterceptLoad(uri: Uri) = false

    /** Returns a URI that should be loaded in place of the given [uri]. */
    open suspend fun getReplacementUrl(uri: Uri) = uri

    /** Asynchronously adds or removes the active tab from the space with given [spaceID]. */
    fun modifySpace(spaceID: String) {
        coroutineScope.launch(dispatchers.io) {
            spaceStore?.addOrRemoveFromSpace(
                spaceID = spaceID,
                url = activeTabModel.urlFlow.value,
                title = activeTabModel.titleFlow.value
            ) ?: Log.e(TAG, "Cannot modify space in Incognito mode")
        }
    }

    /** Dismisses any transient dialogs or popups that are covering the page. */
    fun dismissTransientUi(): Boolean {
        return _activeTabModel.activeTab?.dismissTransientUi() ?: false
    }

    fun canGoBackward(): Boolean {
        return _activeTabModel.navigationInfoFlow.value.canGoBackward
    }
    // endregion

    // region: Find In Page
    fun showFindInPage() {
        _findInPageModel.showFindInPage(_activeTabModel.activeTab)
    }

    fun updateFindInPageQuery(text: String?) {
        _findInPageModel.updateFindInPageQuery(_activeTabModel.activeTab, text)
    }

    fun scrollToFindInPageResult(forward: Boolean) {
        _findInPageModel.scrollToFindInPageResult(_activeTabModel.activeTab, forward)
    }
    // endregion

    companion object {
        val TAG = BrowserWrapper::class.simpleName
    }
}

class CookiePair(val key: String, val value: String)
