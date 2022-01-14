package com.neeva.app.browsing

import android.app.Application
import android.net.Uri
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.neeva.app.NeevaConstants
import com.neeva.app.history.HistoryManager
import com.neeva.app.storage.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.urlbar.URLBarModel
import java.lang.IllegalStateException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserControlsOffsetCallback
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.NewTabType
import org.chromium.weblayer.OpenUrlCallback
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabListCallback

/**
 * Encapsulates everything that is needed to interact with a WebLayer [Browser].
 *
 * [Browser] represents everything required to interact with a set of tabs that are associated with
 * a particular [org.chromium.weblayer.Profile].  Subclasses must be careful to ensure that anything
 * done is allowed by the incognito state defined by the Profile, which means that we are explicitly
 * trying to avoid recording history or automatically firing queries via Apollo (e.g.).
 */
abstract class BrowserWrapper(
    val isIncognito: Boolean,
    val appContext: Application,
    val activityCallbackProvider: () -> ActivityCallbacks?,
    val coroutineScope: CoroutineScope
) {
    data class CreateNewTabInfo(
        val uri: Uri,
        val parentTabId: String? = null
    )

    private val tabList = TabList()
    private val tabCallbackMap: HashMap<Tab, TabCallbacks> = HashMap()

    val orderedTabList: StateFlow<List<TabInfo>>
        get() = tabList.orderedTabList

    private lateinit var fragment: Fragment
    protected var browser: Browser? = null

    val activeTabModel: ActiveTabModel
    val urlBarModel: URLBarModel

    /**
     * Keeps track of data that must be applied to the next tab that is created.  This is necessary
     * because the [Browser] doesn't allow you to create a tab and navigate to somewhere in the same
     * call.  Rather, the [Browser] creates the tab asynchronously and we are expected to detect
     * when it is opened.
     */
    private var newTabInfo: CreateNewTabInfo? = null

    val tabScreenshotManager: TabScreenshotManager by lazy { createTabScreenshotManager() }

    abstract val suggestionsModel: SuggestionsModel?
    abstract val historyManager: HistoryManager?
    abstract val faviconCache: FaviconCache?

    private var tabListRestorer: BrowserRestoreCallback? = null

    init {
        activeTabModel = ActiveTabModel { uri, parentTabId -> createTabWithUri(uri, parentTabId) }
        urlBarModel = URLBarModel(isIncognito, activeTabModel)
    }

    private var fullscreenCallback = FullscreenCallbackImpl(
        { activityCallbackProvider()?.onEnterFullscreen() },
        { flags -> activityCallbackProvider()?.onExitFullscreen(flags) }
    )

    private val tabListCallback = object : TabListCallback() {
        override fun onActiveTabChanged(activeTab: Tab?) {
            activeTabModel.onActiveTabChanged(activeTab)
            tabList.updatedSelectedTab(activeTab?.guid)
        }

        override fun onTabRemoved(tab: Tab) {
            // Delete any screenshot that was taken for the tab.
            val tabId = tab.guid
            coroutineScope.launch(Dispatchers.IO) {
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

                val parentTab = tabInfo?.parentTabId?.let { parentId -> tabList.findTab(parentId) }
                when {
                    parentTab != null -> it.setActiveTab(parentTab)
                    orderedTabList.value.isNotEmpty() -> it.setActiveTab(tabList.getTab(newIndex))
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

    private val browserControlsOffsetCallback = object : BrowserControlsOffsetCallback() {
        override fun onBottomViewOffsetChanged(offset: Int) {
            activityCallbackProvider()?.onBottomBarOffsetChanged(offset)
        }

        override fun onTopViewOffsetChanged(offset: Int) {
            activityCallbackProvider()?.onTopBarOffsetChanged(offset)
        }
    }

    fun initialize() {
        fragment = createBrowserFragment()

        // Have WebLayer Shell retain the fragment instance to simulate the behavior of
        // external embedders (note that if this is changed, then WebLayer Shell should handle
        // rotations and resizes itself via its manifest, as otherwise the user loses all state
        // when the shell is rotated in the foreground).
        fragment.retainInstance = true
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

    @Synchronized
    fun createAndAttachBrowser(fragmentAttacher: (Fragment, Boolean) -> Unit) {
        fragmentAttacher.invoke(fragment, isIncognito)
        if (browser == null) {
            browser = Browser.fromFragment(fragment)
        }
    }

    @CallSuper
    @Synchronized
    open fun registerBrowserCallbacks(): Boolean {
        val browser = this.browser ?: return false
        if (tabListRestorer != null) return false

        val restorer = BrowserRestoreCallbackImpl(browser) {
            createTabWithUri(Uri.parse(NeevaConstants.appURL), parentTabId = null)
        }

        browser.registerTabListCallback(tabListCallback)
        browser.registerBrowserControlsOffsetCallback(browserControlsOffsetCallback)
        browser.registerBrowserRestoreCallback(restorer)

        browser.profile.setTablessOpenUrlCallback(
            object : OpenUrlCallback() {
                override fun getBrowserForNewTab() = browser
                override fun onTabAdded(tab: Tab) = registerTabCallbacks(tab)
            }
        )

        tabListRestorer = restorer
        return true
    }

    /** Prepares a Browser to be displayed and used within the current environment. */
    fun prepareBrowser(
        topControlsPlaceholder: View,
        bottomControlsPlaceholder: View
    ): Fragment {
        val browser = this.browser ?: throw IllegalStateException()
        registerBrowserCallbacks()

        activityCallbackProvider()?.getDisplaySize()?.let { windowSize ->
            browser.setMinimumSurfaceSize(windowSize.x, windowSize.y)
        }

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

        return fragment
    }

    /** Called when we detect that the logged in user's auth token has been updated. */
    open fun onAuthTokenUpdated() {}

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
            appContext = appContext,
            coroutineScope = coroutineScope,
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
        activeTabModel.onActiveTabChanged(null)

        browser = null
        tabListRestorer = null
    }

    /**
     * Creates a new tab and shows the given [uri].
     *
     * Because [Browser] only allows you to create a Tab without a URL, we save the URL for when
     * the [TabListCallback] tells us the new tab has been added.
     */
    fun createTabWithUri(uri: Uri, parentTabId: String?) {
        browser?.let {
            newTabInfo = CreateNewTabInfo(uri, parentTabId)
            it.createTab()
        }
    }

    fun canExitFullscreen() = fullscreenCallback.canExitFullscreen()
    fun exitFullscreen() = fullscreenCallback.exitFullscreen()

    private fun onNewTabAdded(tab: Tab) {
        tabList.add(tab, tabScreenshotManager.getTabScreenshotFile(tab).toUri())
        registerTabCallbacks(tab)
    }

    fun closeTab(primitive: TabInfo) = tabList.findTab(primitive.id)?.dispatchBeforeUnloadAndClose()

    fun selectTab(primitive: TabInfo) = tabList.findTab(primitive.id)?.let { selectTab(it) }

    fun selectTab(tab: Tab) {
        // Screenshot the previous tab right before it is replaced to keep it as fresh as possible.
        takeScreenshotOfActiveTab()

        tab.browser.setActiveTab(tab)
    }

    fun takeScreenshotOfActiveTab(onCompleted: () -> Unit = {}) {
        val tab = browser?.activeTab
        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
    }

    /**
     * Closes the active Tab if it is a child of another Tab.  No-op if the active tab is not a
     * child of another Tab.
     *
     * @return True if the tab was the child of an existing Tab.
     */
    fun closeActiveChildTab(): Boolean {
        return browser?.activeTab
            ?.let { activeTab ->
                val tabInfo = tabList.getTabInfo(activeTab.guid)
                tabInfo?.parentTabId?.let {
                    closeTab(tabInfo)
                    true
                }
            } ?: false
    }

    fun hasNoTabs(): Boolean = tabList.hasNoTabs()
}
