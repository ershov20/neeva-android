package com.neeva.app.browsing

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.history.HistoryManager
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.urlbar.URLBarModelImpl
import java.lang.IllegalStateException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserFragment
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.CookieManager
import org.chromium.weblayer.FaviconFetcher
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Profile
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabListCallback
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class BrowserWrapperTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var browser: Browser
    private lateinit var browserWrapper: BrowserWrapper
    private lateinit var context: Context
    private lateinit var dispatchers: Dispatchers
    private lateinit var profile: Profile
    private lateinit var urlBarModel: URLBarModelImpl

    // Default mocks automatically initialized via Mockito.mockitoSession().initMocks().
    @Mock private lateinit var activeTabModel: ActiveTabModelImpl
    @Mock private lateinit var activityCallbacks: ActivityCallbacks
    @Mock private lateinit var browserFragment: BrowserFragment
    @Mock private lateinit var findInPageModel: FindInPageModelImpl
    @Mock private lateinit var cookieManager: CookieManager
    @Mock private lateinit var faviconCache: FaviconCache
    @Mock private lateinit var fragmentAttacher: (fragment: Fragment, isIncognito: Boolean) -> Unit
    @Mock private lateinit var historyManager: HistoryManager
    @Mock private lateinit var spaceStore: SpaceStore
    @Mock private lateinit var suggestionsModel: SuggestionsModel
    @Mock private lateinit var tabScreenshotManager: TabScreenshotManager

    private lateinit var topPlaceholder: View
    private lateinit var bottomPlaceholder: View
    private lateinit var urlBarModelIsEditing: MutableStateFlow<Boolean>
    private lateinit var mockTabs: MutableList<Tab>

    private var shouldInterceptLoad: Boolean = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun setUp() {
        super.setUp()

        context = ApplicationProvider.getApplicationContext()

        topPlaceholder = createViewPlaceholder()
        bottomPlaceholder = createViewPlaceholder()
        urlBarModelIsEditing = MutableStateFlow(false)
        mockTabs = mutableListOf()

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )

        profile = mock {
            on { getCookieManager() } doReturn cookieManager
        }

        browser = mock {
            on { createTab() } doAnswer {
                createMockTab().also {
                    // WebLayer synchronously fires the `onTabAdded` callback before returning the
                    // new Tab.
                    val tabListCallbackCaptor = argumentCaptor<TabListCallback>()
                    verify(browser).registerTabListCallback(tabListCallbackCaptor.capture())
                    tabListCallbackCaptor.lastValue.onTabAdded(it)
                }
            }

            on { getProfile() } doReturn profile
            on { getTabs() } doReturn mockTabs.toSet()
            on { isDestroyed() } doReturn false
            on { isRestoringPreviousState() } doReturn true
        }

        urlBarModel = mock {
            on { isEditing } doReturn urlBarModelIsEditing
        }

        browserWrapper = object : BrowserWrapper(
            isIncognito = false,
            appContext = context,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = dispatchers,
            activityCallbackProvider = { activityCallbacks },
            suggestionsModel = suggestionsModel,
            faviconCache = faviconCache,
            spaceStore = spaceStore,
            _activeTabModel = activeTabModel,
            _urlBarModel = urlBarModel,
            _findInPageModel = findInPageModel
        ) {
            override val historyManager = this@BrowserWrapperTest.historyManager

            override fun createTabScreenshotManager() = this@BrowserWrapperTest.tabScreenshotManager

            override fun createBrowserFragment(): Fragment = this@BrowserWrapperTest.browserFragment

            override fun getBrowserFromFragment(fragment: Fragment): Browser {
                return this@BrowserWrapperTest.browser
            }

            override fun shouldInterceptLoad(uri: Uri) = shouldInterceptLoad

            override suspend fun getReplacementUrl(uri: Uri): Uri {
                if (shouldInterceptLoad(uri)) {
                    return uri.buildUpon().appendPath("incognito_redirect").build()
                } else {
                    throw IllegalStateException()
                }
            }
        }
    }

    /** Creates a View with LayoutParams that can be edited. */
    private fun createViewPlaceholder(): View = mock {
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        on { getLayoutParams() } doReturn layoutParams
    }

    private fun createMockTab(): Tab {
        val navigationController: NavigationController = mock()
        val faviconFetcher: FaviconFetcher = mock()
        val mockTab: Tab = mock {
            on { getGuid() } doReturn "tab guid ${mockTabs.size}"
            on { getNavigationController() } doReturn navigationController
            on { getBrowser() } doReturn browser
            on { createFaviconFetcher(any()) } doReturn faviconFetcher
        }

        mockTabs.add(mockTab)
        return mockTab
    }

    /**
     * Say that restoration has completed, allowing URL loading to continue.  Because the
     * Browser has 0 tabs, the BrowserRestoreCallbackImpl will create one for https://neeva.com.
     */
    private fun completeBrowserRestoration() {
        val restoreCallbackCaptor = argumentCaptor<BrowserRestoreCallback>()
        verify(browser).registerBrowserRestoreCallback(restoreCallbackCaptor.capture())
        restoreCallbackCaptor.lastValue.onRestoreCompleted()
        coroutineScopeRule.scope.advanceUntilIdle()

        verify(browser, times(1)).createTab()
        verify(mockTabs.last().navigationController).navigate(eq(Uri.parse(NeevaConstants.appURL)))
    }

    @Test
    fun loadUrl_withIntercept_loadsRedirectedUrlInActiveTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        val redirectUri = Uri.parse("https://www.example.com/incognito_redirect")

        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user's Browser currently has an active tab open.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        Mockito.`when`(activeTabModel.activeTab).thenReturn(mockTabs.last())

        // Say that the load should be intercepted, then load the URL.
        shouldInterceptLoad = true
        browserWrapper.loadUrl(expectedUri)
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore)
        verify(activeTabModel, never()).loadUrlInActiveTab(eq(expectedUri))
        verify(activeTabModel).loadUrlInActiveTab(eq(redirectUri))
        verify(browser, times(1)).createTab()
    }

    @Test
    fun createAndAttachBrowser_hooksIntoAndroidViewHierarchy() {
        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        verify(browser).setTopView(topPlaceholder)
        verify(browser).setBottomView(bottomPlaceholder)
        verify(browser).setMinimumSurfaceSize(100, 200)
        verify(fragmentAttacher).invoke(eq(browserFragment), eq(false))

        expectThat(topPlaceholder.layoutParams.height).isNotEqualTo(LayoutParams.MATCH_PARENT)
        expectThat(bottomPlaceholder.layoutParams.height).isNotEqualTo(LayoutParams.MATCH_PARENT)
    }

    @Test
    fun loadUrl_beforeRestorationCompletes_eventuallyLoadsUrlInActiveTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // Load a URL should open it in a new tab, but restoration hasn't completed yet.
        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = true,
            isViaIntent = false,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        verify(activeTabModel, never()).loadUrlInActiveTab(eq(Uri.parse(NeevaConstants.appURL)))
        verify(activeTabModel, never()).loadUrlInActiveTab(eq(expectedUri))

        // Finish browser restoration.  It should have allowed the blocked URL to load and create
        // a new tab.
        val restoreCallbackCaptor = argumentCaptor<BrowserRestoreCallback>()
        verify(browser).registerBrowserRestoreCallback(restoreCallbackCaptor.capture())
        restoreCallbackCaptor.lastValue.onRestoreCompleted()
        coroutineScopeRule.scope.advanceUntilIdle()

        verify(browser, times(2)).createTab()
        verify(mockTabs.first().navigationController).navigate(eq(Uri.parse(NeevaConstants.appURL)))
        verify(mockTabs.last().navigationController).navigate(eq(expectedUri))
        expectThat(browserWrapper.orderedTabList.value).hasSize(2)
    }

    @Test
    fun loadUrl_withActiveTabAndWithoutInterceptOrNewTabRequirement_loadsUrlInActiveTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user's Browser currently has an active tab open.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        Mockito.`when`(activeTabModel.activeTab).thenReturn(mockTabs.last())

        // Loading a URL should open it in the existing tab.
        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = false,
            isViaIntent = false,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore)
        verify(activeTabModel).loadUrlInActiveTab(eq(expectedUri))
        verify(browser, times(1)).createTab()
    }

    @Test
    fun loadUrl_withoutActiveTabOrInterceptOrNewTabRequirement_loadsUrlInNewTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user's Browser currently has no active tabs open.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        Mockito.`when`(activeTabModel.activeTab).thenReturn(null)

        // Load a URL in a tab without a parent.
        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = false,
            isViaIntent = false,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab.
        verify(activeTabModel, never()).loadUrlInActiveTab(any())
        verify(browser, times(2)).createTab()

        val tab: Tab = mockTabs.last()
        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore + 1)
        verify(tab.navigationController).navigate(eq(expectedUri))

        // Check that the tab's properties were saved.
        val tabInfoList = browserWrapper.orderedTabList.value.filter { it.id == tab.guid }
        expectThat(tabInfoList).hasSize(1)
        expectThat(tabInfoList[0].data.parentTabId).isNull()
        expectThat(tabInfoList[0].data.openType).isEqualTo(TabInfo.TabOpenType.DEFAULT)
    }

    @Test
    fun loadUrl_askingForNewTab_createsNewTabWithUrl() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        completeBrowserRestoration()

        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = true,
            isViaIntent = false,
            parentTabId = "parent tab id"
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab and then navigate to the URL.
        verify(activeTabModel, never()).loadUrlInActiveTab(any())
        verify(browser, times(2)).createTab()

        val tab: Tab = mockTabs.last()
        verify(tab.navigationController).navigate(eq(expectedUri))

        // Fire the callback and say that the Browser finished adding a tab.  The BrowserWrapper
        // should see that a tab is pending and open the URL to it.
        val tabListCallbackCaptor = argumentCaptor<TabListCallback>()
        verify(browser).registerTabListCallback(tabListCallbackCaptor.capture())
        tabListCallbackCaptor.lastValue.onTabAdded(tab)

        // Check that the tab's properties were saved.
        val tabInfoList = browserWrapper.orderedTabList.value.filter { it.id == tab.guid }
        expectThat(tabInfoList).hasSize(1)
        expectThat(tabInfoList[0].data.parentTabId).isEqualTo("parent tab id")
        expectThat(tabInfoList[0].data.openType).isEqualTo(TabInfo.TabOpenType.CHILD_TAB)
    }

    @Test
    fun loadUrl_askingForNewTabViaIntent_createsNewTabWithUrl() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = true,
            isViaIntent = true,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab.
        val tabListCallbackCaptor = argumentCaptor<TabListCallback>()
        verify(activeTabModel, never()).loadUrlInActiveTab(any())
        verify(browser, times(2)).createTab()
        verify(browser).registerTabListCallback(tabListCallbackCaptor.capture())

        // Fire the callback and say that the Browser finished adding a tab.  The BrowserWrapper
        // should see that a tab is pending and open the URL to it.
        val tab: Tab = mockTabs.last()
        tabListCallbackCaptor.lastValue.onTabAdded(tab)
        verify(tab.navigationController).navigate(eq(expectedUri))

        // Check that the tab's properties were saved.
        val tabInfoList = browserWrapper.orderedTabList.value.filter { it.id == tab.guid }
        expectThat(tabInfoList).hasSize(1)
        expectThat(tabInfoList[0].data.parentTabId).isNull()
        expectThat(tabInfoList[0].data.openType).isEqualTo(TabInfo.TabOpenType.VIA_INTENT)
    }

    @Test
    fun browserRestoreCallback_withNoTabs_createsNeevaTab() {
        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        val restoreCallbackCaptor = argumentCaptor<BrowserRestoreCallback>()
        verify(browser).registerBrowserRestoreCallback(restoreCallbackCaptor.capture())
        restoreCallbackCaptor.lastValue.onRestoreCompleted()
        coroutineScopeRule.scope.advanceUntilIdle()
    }

    @Test
    fun loadUrl_withLazyTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Opening a lazy tab should tell the URL bar to take focus so that the user can see zero
        // query and the other suggestions.
        browserWrapper.openLazyTab()
        verify(urlBarModel).onRequestFocus()

        // Load a URL without explicitly saying it should be in a new tab.
        browserWrapper.loadUrl(uri = expectedUri)
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab and then navigate to the URL.
        val tab = mockTabs.last()
        verify(activeTabModel, never()).loadUrlInActiveTab(any())
        verify(browser, times(2)).createTab()
        verify(tab.navigationController).navigate(eq(expectedUri))

        // Fire the callback and say that the Browser finished adding a tab.  The BrowserWrapper
        // should see that a tab is pending and open the URL to it.
        val tabListCallbackCaptor = argumentCaptor<TabListCallback>()
        verify(browser).registerTabListCallback(tabListCallbackCaptor.capture())
        tabListCallbackCaptor.lastValue.onTabAdded(tab)

        // Check that the tab's properties were saved.
        val tabInfoList = browserWrapper.orderedTabList.value.filter { it.id == tab.guid }
        expectThat(tabInfoList).hasSize(1)
        expectThat(tabInfoList[0].data.parentTabId).isNull()
        expectThat(tabInfoList[0].data.openType).isEqualTo(TabInfo.TabOpenType.DEFAULT)
    }

    @Test
    fun lazyTab_tracksEditingState() {
        val expectedUri = Uri.parse("https://www.example.com")
        browserWrapper.createAndAttachBrowser(
            topPlaceholder,
            bottomPlaceholder,
            Rect(0, 0, 100, 200),
            fragmentAttacher
        )
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user has an active tab.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        Mockito.`when`(activeTabModel.activeTab).thenReturn(mockTabs.last())

        // Opening a lazy tab should tell the URL bar to take focus so that the user can see zero
        // query and the other suggestions.
        browserWrapper.openLazyTab()
        verify(urlBarModel).onRequestFocus()
        urlBarModelIsEditing.value = true

        // Say that the URL bar lost focus.
        coroutineScopeRule.scope.advanceUntilIdle()
        urlBarModelIsEditing.value = false
        coroutineScopeRule.scope.advanceUntilIdle()

        // Load a URL.  Because the lazy tab state was lost, the tab should be opened up in the
        // existing tab.
        browserWrapper.loadUrl(uri = expectedUri)
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore)
        verify(activeTabModel).loadUrlInActiveTab(eq(expectedUri))
        verify(browser, times(1)).createTab()
    }
}
