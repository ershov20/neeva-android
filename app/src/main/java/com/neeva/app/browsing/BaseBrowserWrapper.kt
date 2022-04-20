package com.neeva.app.browsing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.findinpage.FindInPageModel
import com.neeva.app.browsing.findinpage.FindInPageModelImpl
import com.neeva.app.history.HistoryManager
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.urlbar.URLBarModelImpl
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserControlsOffsetCallback
import org.chromium.weblayer.BrowserEmbeddabilityMode
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.NewTabType
import org.chromium.weblayer.OpenUrlCallback
import org.chromium.weblayer.PageInfoDisplayOptions
import org.chromium.weblayer.Profile
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabListCallback

abstract class BaseBrowserWrapper internal constructor(
    override val isIncognito: Boolean,
    protected val appContext: Context,
    protected val coroutineScope: CoroutineScope,
    protected val dispatchers: Dispatchers,
    protected val activityCallbackProvider: () -> ActivityCallbacks?,
    override val suggestionsModel: SuggestionsModel?,
    final override val faviconCache: FaviconCache,
    protected val spaceStore: SpaceStore?,
    private val _activeTabModel: ActiveTabModelImpl,
    private val _urlBarModel: URLBarModelImpl,
    private val _findInPageModel: FindInPageModelImpl,
    private val historyManager: HistoryManager?,
    private val tabScreenshotManager: TabScreenshotManager
) : BrowserWrapper, FaviconCache.ProfileProvider {
    constructor(
        isIncognito: Boolean,
        appContext: Context,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        activityCallbackProvider: () -> ActivityCallbacks?,
        suggestionsModel: SuggestionsModel?,
        faviconCache: FaviconCache,
        spaceStore: SpaceStore?,
        historyManager: HistoryManager?,
        tabScreenshotManager: TabScreenshotManager
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
        _findInPageModel = FindInPageModelImpl(),
        historyManager = historyManager,
        tabScreenshotManager = tabScreenshotManager
    )

    private val tabList = TabList()
    private val tabCallbackMap: HashMap<Tab, TabCallbacks> = HashMap()

    final override val orderedTabList: StateFlow<List<TabInfo>> get() = tabList.orderedTabList

    /** Tracks if the active tab needs to be reloaded due to a renderer crash. */
    override val shouldDisplayCrashedTab: Flow<Boolean> =
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

    override val activeTabModel: ActiveTabModel get() = _activeTabModel
    override val findInPageModel: FindInPageModel get() = _findInPageModel
    override val urlBarModel: URLBarModel get() = _urlBarModel

    /** Tracks whether the user needs to be kept in the CardGrid if they're on that screen. */
    final override val userMustStayInCardGridFlow: StateFlow<Boolean>

    private var tabListRestorer: BrowserRestoreCallback? = null

    private val _isLazyTabFlow = MutableStateFlow(false)
    override val isLazyTabFlow: StateFlow<Boolean> get() = _isLazyTabFlow

    /** Tracks when the WebLayer [Browser] has finished restoration and the [tabList] is ready. */
    private val isBrowserReady = CompletableDeferred<Boolean>()

    init {
        faviconCache.profileProvider = FaviconCache.ProfileProvider { getProfile() }

        userMustStayInCardGridFlow = orderedTabList
            .combine(_isLazyTabFlow) { tabs, isLazyTab -> tabs.isEmpty() && !isLazyTab }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

        coroutineScope.launch {
            urlBarModel.isEditing.collectLatest { isEditing ->
                _isLazyTabFlow.value = _isLazyTabFlow.value && isEditing
            }
        }

        coroutineScope.launch {
            browserFlow.collectLatest { _urlBarModel.onBrowserChanged(it) }
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

    /** Returns the [Browser] from the given [fragment]. */
    internal open fun getBrowserFromFragment(fragment: Fragment): Browser? {
        return Browser.fromFragment(fragment)
    }

    /**
     * Creates a Fragment that contains the [Browser] used to interface with WebLayer.
     *
     * This [Browser] that is created must be associated with the correct incognito or non-incognito
     * profile to avoid leaking state.
     */
    abstract fun createBrowserFragment(): Fragment

    /** Prepares the WebLayer Browser to interface with our app. */
    override fun createAndAttachBrowser(
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

        // Set the Views that WebLayer will use as placeholders for our toolbar.
        //
        // The [Job] usage is a workaround for https://github.com/neevaco/neeva-android/issues/452:
        // If we try to set the Browser's toolbar placeholders during initialization, we will
        // occasionally trigger an odd race condition that cause a Null Pointer Exception somewhere
        // deep in native WebLayer code.
        //
        // To work around this, we start a one-off Job that waits until we see that WebLayer has
        // registered a navigation, which we take as a signal that the [Browser] and its
        // [ContentViewRenderView] have been initialized.
        var toolbarJob: Job? = null
        toolbarJob = activeTabModel.navigationInfoFlow
            .filterNot { it.navigationListSize == 0 }
            .onEach {
                setToolbarPlaceholders()
                toolbarJob?.cancel()
                toolbarJob = null
            }
            .launchIn(coroutineScope)
    }

    /**
     * Sets the top and bottom toolbar placeholders that WebLayer will use to perform its toolbar
     * auto-hiding logic.
     *
     * We need to use placeholders because WebLayer does not work well with Jetpack Compose: they
     * don't get rendered at all, meaning that our toolbars are replaced with white boxes when the
     * toolbars aren't fully visible.
     *
     * Instead, [org.chromium.weblayer.BrowserControlsOffsetCallback] suggests that we pass in
     * placeholders that are the same height as the real toolbars and offset the toolbars whenever
     * the callback fires.
     */
    private fun setToolbarPlaceholders() {
        val browser = browserFlow.value ?: return
        val topControlsPlaceholder = View(appContext)
        val bottomControlsPlaceholder = View(appContext)

        browser.setTopView(topControlsPlaceholder)
        browser.setBottomView(bottomControlsPlaceholder)

        // The placeholders are now in the View hierarchy, so they now have LayoutParams that we can
        // set to our desired toolbar heights.
        topControlsPlaceholder.layoutParams.height =
            appContext.resources.getDimensionPixelSize(R.dimen.top_toolbar_height)
        bottomControlsPlaceholder.layoutParams.height =
            appContext.resources.getDimensionPixelSize(R.dimen.bottom_toolbar_height)

        topControlsPlaceholder.requestLayout()
        bottomControlsPlaceholder.requestLayout()
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
                    isViaIntent = false,
                    stayInApp = true
                )
            },
            afterRestoreCompleted = { isBrowserReady.complete(true) }
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
            // if the app dies in the background, with WebLayer's Fragments automatically creating
            // the Browser before we have a chance to hook into it.
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
    private fun createTabWithUri(
        uri: Uri,
        parentTabId: String?,
        isViaIntent: Boolean,
        stayInApp: Boolean
    ) {
        browser?.let {
            val tabOpenType = when {
                parentTabId != null -> TabInfo.TabOpenType.CHILD_TAB
                isViaIntent -> TabInfo.TabOpenType.VIA_INTENT
                else -> TabInfo.TabOpenType.DEFAULT
            }

            val newTab = it.createTab()
            newTab.navigate(uri, stayInApp)

            // onTabAdded should have been called by this point, allowing us to store the extra
            // information about the Tab.
            tabList.updateParentInfo(
                tab = newTab,
                parentTabId = parentTabId,
                tabOpenType = tabOpenType
            )

            selectTab(newTab, takeScreenshotBeforeSelecting = false)
        }
    }

    private fun onNewTabAdded(tab: Tab) {
        tabList.add(tab)
        registerTabCallbacks(tab)
    }

    override fun closeTab(primitive: TabInfo) {
        tabList.findTab(primitive.id)?.dispatchBeforeUnloadAndClose()
    }

    override fun closeAllTabs() {
        tabList.forEach { it.dispatchBeforeUnloadAndClose() }
    }

    override fun selectTab(primitive: TabInfo) {
        tabList.findTab(primitive.id)?.let { selectTab(it) }
    }

    private fun selectTab(tab: Tab, takeScreenshotBeforeSelecting: Boolean = true) {
        if (takeScreenshotBeforeSelecting) {
            // Screenshot the previous tab right before replacement to keep it as fresh as possible.
            // You may not want to do this in cases where the WebLayer's View is the wrong height,
            // which can happen if the keyboard is up.
            // TODO(dan.alcantara): Find a better way of handling
            //                      https://github.com/neevaco/neeva-android/issues/218
            takeScreenshotOfActiveTab()
        }

        browser?.setActiveTab(tab)
    }

    override fun restoreScreenshotOfTab(tabId: String): Bitmap? {
        return tabScreenshotManager.restoreScreenshot(tabId)
    }

    override fun takeScreenshotOfActiveTab(onCompleted: () -> Unit) {
        val tab = browser?.activeTab
        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
    }

    override fun showPageInfo() {
        browserFlow.value?.urlBarController?.showPageInfo(
            PageInfoDisplayOptions.builder().build()
        )
    }

    /**
     * Closes the active Tab if and only if it was opened via a VIEW Intent.
     * @return True if the tab was closed.
     */
    override fun closeActiveTabIfOpenedViaIntent(): Boolean {
        return conditionallyCloseActiveTab(TabInfo.TabOpenType.VIA_INTENT)
    }

    /**
     * Closes the active Tab if and only if it was opened as a child of another Tab.
     * @return True if the tab was closed.
     */
    override fun closeActiveChildTab(): Boolean {
        return conditionallyCloseActiveTab(TabInfo.TabOpenType.CHILD_TAB)
    }

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
    override fun openLazyTab() {
        _isLazyTabFlow.value = true
        urlBarModel.requestFocus()
    }

    /** Returns true if the [Browser] is maintaining no tabs. */
    override fun hasNoTabs(): Boolean = tabList.hasNoTabs()
    override fun hasNoTabsFlow(): Flow<Boolean> = tabList.hasNoTabsFlow

    /** Returns true if the user should be forced to go to the card grid. */
    override fun userMustBeShownCardGrid(): Boolean = tabList.hasNoTabs() && !_isLazyTabFlow.value

    override fun isFullscreen(): Boolean = fullscreenCallback.isFullscreen()
    override fun exitFullscreen(): Boolean = fullscreenCallback.exitFullscreen()

    /** Provides access to the WebLayer profile. */
    override fun getProfile(): Profile? = browser?.profile

    /** Returns a list of cookies split by key and values. */
    override fun getCookiePairs(uri: Uri, callback: (List<CookiePair>) -> Unit) {
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
    override fun goBack() = _activeTabModel.goBack()
    override fun goForward() = _activeTabModel.goForward()
    override fun reload() = _activeTabModel.reload()

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
    override fun loadUrl(
        uri: Uri,
        inNewTab: Boolean,
        isViaIntent: Boolean,
        parentTabId: String?,
        stayInApp: Boolean,
        onLoadStarted: () -> Unit
    ) = coroutineScope.launch {
        // If you try to load a URL in a new tab before restoration has completed, the Browser may
        // drop the request on the floor.
        waitUntilBrowserIsReady()

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
                isViaIntent = isViaIntent,
                stayInApp = stayInApp
            )
        } else {
            _activeTabModel.loadUrlInActiveTab(urlToLoad, stayInApp)
        }

        urlBarModel.clearFocus()
        onLoadStarted()
    }

    /** Asynchronously adds or removes the active tab from the space with given [spaceID]. */
    override fun modifySpace(spaceID: String) {
        coroutineScope.launch(dispatchers.io) {
            spaceStore?.addOrRemoveFromSpace(
                spaceID = spaceID,
                url = activeTabModel.urlFlow.value,
                title = activeTabModel.titleFlow.value
            ) ?: Log.e(TAG, "Cannot modify space in Incognito mode")
        }
    }

    /** Dismisses any transient dialogs or popups that are covering the page. */
    override fun dismissTransientUi(): Boolean {
        return _activeTabModel.activeTab?.dismissTransientUi() ?: false
    }

    override fun canGoBackward(): Boolean {
        return _activeTabModel.navigationInfoFlow.value.canGoBackward
    }
    // endregion

    // region: Find In Page
    override fun showFindInPage() {
        _activeTabModel.activeTab?.let { _findInPageModel.showFindInPage(it) }
    }
    // endregion

    override suspend fun allowScreenshots(allowScreenshots: Boolean) {
        val mode = if (allowScreenshots) {
            BrowserEmbeddabilityMode.SUPPORTED
        } else {
            BrowserEmbeddabilityMode.UNSUPPORTED
        }

        val result = suspendCoroutine<Boolean?> { continuation ->
            browser?.setEmbeddabilityMode(mode) {
                continuation.resume(it)
            } ?: run {
                continuation.resume(null)
            }
        }

        if (result != true) {
            Log.e(TAG, "Failed to update mode (allowScreenshots = $allowScreenshots)")
        }
    }

    /** Suspends the coroutine until the browser has finished initialization and restoration. */
    override suspend fun waitUntilBrowserIsReady(): Boolean {
        return isBrowserReady.await()
    }

    companion object {
        val TAG = BrowserWrapper::class.simpleName
    }
}
