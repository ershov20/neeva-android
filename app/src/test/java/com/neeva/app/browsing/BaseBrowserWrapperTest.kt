package com.neeva.app.browsing

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.browsing.findinpage.FindInPageModelImpl
import com.neeva.app.browsing.urlbar.URLBarModelImpl
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.cookiecutter.ScriptInjectionManager
import com.neeva.app.createMockNavigationController
import com.neeva.app.history.HistoryManager
import com.neeva.app.neevascope.NeevascopeModel
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import java.util.EnumSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserFragment
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.ContentFilterManager
import org.chromium.weblayer.CookieManager
import org.chromium.weblayer.FaviconFetcher
import org.chromium.weblayer.NavigateParams
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
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BaseBrowserWrapperTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var activeTabModelImpl: ActiveTabModelImpl
    private lateinit var browser: Browser
    private lateinit var browserFragment: BrowserFragment
    private lateinit var browserWrapper: BrowserWrapper
    private lateinit var context: Context
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var tabList: TabList
    private lateinit var profile: Profile
    private lateinit var urlBarModel: URLBarModelImpl

    // Default mocks automatically initialized via Mockito.mockitoSession().initMocks().
    @Mock private lateinit var activityCallbackProvider: ActivityCallbackProvider
    @Mock private lateinit var contentFilterManager: ContentFilterManager
    @Mock private lateinit var cookieCutterModel: CookieCutterModel
    @Mock private lateinit var cookieManager: CookieManager
    @Mock private lateinit var domainProvider: DomainProvider
    @Mock private lateinit var faviconCache: FaviconCache
    @Mock private lateinit var findInPageModel: FindInPageModelImpl
    @Mock private lateinit var fragmentAttacher: (fragment: Fragment, isIncognito: Boolean) -> Unit
    @Mock private lateinit var historyManager: HistoryManager
    @Mock private lateinit var settingsDataModel: SettingsDataModel
    @Mock private lateinit var spaceStore: SpaceStore
    @Mock private lateinit var suggestionsModel: SuggestionsModel
    @Mock private lateinit var neevascopeModel: NeevascopeModel
    @Mock private lateinit var tabScreenshotManager: TabScreenshotManager
    @Mock private lateinit var scriptInjectionManager: ScriptInjectionManager
    @Mock private lateinit var popupModel: PopupModel
    @Mock private lateinit var neevaUser: NeevaUser

    private lateinit var navigationInfoFlow: MutableStateFlow<ActiveTabModel.NavigationInfo>
    private lateinit var urlBarModelStateFlow: MutableStateFlow<URLBarModelState>
    private lateinit var mockTabs: MutableList<Tab>

    private var activeTab: Tab? = null
    private var shouldInterceptLoad: Boolean = false
    private var wasBlankTabCreated: Boolean = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()
        context = ApplicationProvider.getApplicationContext()
        tabList = TabList()

        navigationInfoFlow = MutableStateFlow(ActiveTabModel.NavigationInfo())
        urlBarModelStateFlow = MutableStateFlow(URLBarModelState())
        mockTabs = mutableListOf()

        activeTabModelImpl = mock {
            var currentActiveTab: Tab? = null

            on { navigationInfoFlow } doReturn navigationInfoFlow
            on { activeTab } doAnswer { currentActiveTab }
            on { onActiveTabChanged(any()) } doAnswer {
                currentActiveTab = it.arguments[0] as Tab?
            }
        }

        profile = mock {
            on { contentFilterManager } doReturn contentFilterManager
            on { cookieManager } doReturn cookieManager
        }

        browserFragment = mock {
            on { viewLifecycleOwnerLiveData } doReturn MutableLiveData(null)
        }

        browser = mock {
            on { setActiveTab(any()) } doAnswer {
                activeTab = it.arguments[0] as Tab
                getTabListCallback().onActiveTabChanged(activeTab)
            }

            on { activeTab } doAnswer { activeTab }

            on { createTab() } doAnswer {
                createMockTab().also {
                    // WebLayer synchronously fires the `onTabAdded` callback before returning the
                    // new Tab.
                    getTabListCallback().onTabAdded(it)
                }
            }

            on { profile } doReturn profile
            on { tabs } doAnswer { mockTabs.toSet() }
            on { isDestroyed } doReturn false
            on { isRestoringPreviousState } doReturn true

            on { setTopView(any()) } doAnswer { attachViewToParent(it.arguments[0] as View) }
            on { setBottomView(any()) } doAnswer { attachViewToParent(it.arguments[0] as View) }
        }

        urlBarModel = mock {
            on { stateFlow } doAnswer { urlBarModelStateFlow }
            on { isLazyTab } doReturn(
                urlBarModelStateFlow.map { it.isLazyTab }.distinctUntilChanged()
                )
        }

        cookieCutterModel = mock {
            on { trackingDataFlow } doReturn MutableStateFlow(null)
            on { enableTrackingProtection } doReturn mutableStateOf(true)
            on { cookieNoticeBlockedFlow } doReturn MutableStateFlow(false)
            on { cookieCuttingPreferences } doReturn mutableStateOf(
                EnumSet.noneOf(CookieCutterModel.CookieNoticeCookies::class.java)
            )
            on { enableCookieNoticeSuppression } doReturn mutableStateOf(true)
        }

        scriptInjectionManager = mock()

        browserWrapper = object : BaseBrowserWrapper(
            isIncognito = false,
            appContext = context,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            activityCallbackProvider = activityCallbackProvider,
            suggestionsModel = suggestionsModel,
            neevascopeModel = neevascopeModel,
            popupModel = popupModel,
            neevaUser = neevaUser,
            faviconCache = faviconCache,
            spaceStore = spaceStore,
            tabList = tabList,
            _activeTabModelImpl = activeTabModelImpl,
            _urlBarModel = urlBarModel,
            _findInPageModel = findInPageModel,
            historyManager = historyManager,
            tabScreenshotManager = tabScreenshotManager,
            domainProvider = domainProvider,
            neevaConstants = neevaConstants,
            settingsDataModel = settingsDataModel,
            cookieCutterModel = cookieCutterModel,
            scriptInjectionManager = scriptInjectionManager
        ) {
            override fun createBrowserFragment(): Fragment =
                this@BaseBrowserWrapperTest.browserFragment

            override fun getBrowserFromFragment(fragment: Fragment): Browser {
                return this@BaseBrowserWrapperTest.browser
            }

            override fun shouldInterceptLoad(uri: Uri) = shouldInterceptLoad

            override suspend fun getReplacementUrl(uri: Uri): Uri {
                if (shouldInterceptLoad(uri)) {
                    return uri.buildUpon().appendPath("incognito_redirect").build()
                } else {
                    throw IllegalStateException()
                }
            }

            override fun onBlankTabCreated(tab: Tab) {
                wasBlankTabCreated = true
            }
        }
    }

    private fun getTabListCallback(): TabListCallback {
        val tabListCallbackCaptor = argumentCaptor<TabListCallback>()
        verify(browser).registerTabListCallback(tabListCallbackCaptor.capture())
        return tabListCallbackCaptor.lastValue
    }

    private fun attachViewToParent(view: View) {
        FrameLayout(context).apply { addView(view) }
    }

    private fun createMockTab(): Tab {
        val navigationController = createMockNavigationController()
        val faviconFetcher: FaviconFetcher = mock()
        val mockTab: Tab = mock {
            on { guid } doReturn "tab guid ${mockTabs.size}"
            on { getNavigationController() } doReturn navigationController
            on { browser } doReturn browser
            on { createFaviconFetcher(any()) } doReturn faviconFetcher
        }

        // When WebLayer closes a Tab, the Browser fires a callback which lets us know that it
        // should be removed from our tab list.
        Mockito.`when`(mockTab.dispatchBeforeUnloadAndClose()).doAnswer {
            mockTabs.remove(mockTab)
            getTabListCallback().onTabRemoved(mockTab)
        }

        mockTabs.add(mockTab)
        return mockTab
    }

    private fun createAndAttachBrowser() {
        browserWrapper.createAndAttachBrowser(
            displaySize = Rect(0, 0, 100, 200),
            toolbarConfiguration = MutableStateFlow(ToolbarConfiguration()),
            fragmentAttacher = fragmentAttacher
        )
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

        val urlCaptor = argumentCaptor<Uri>()
        val navigateParamsCaptor = argumentCaptor<NavigateParams>()
        verify(browser, times(1)).createTab()
        verify(mockTabs.last().navigationController).navigate(
            urlCaptor.capture(),
            navigateParamsCaptor.capture()
        )
        expectThat(urlCaptor.lastValue).isEqualTo(Uri.parse(neevaConstants.appURL))
        expectThat(navigateParamsCaptor.lastValue.isIntentProcessingDisabled).isEqualTo(true)
    }

    @Test
    fun loadUrl_withIntercept_loadsRedirectedUrlInActiveTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        val redirectUri = Uri.parse("https://www.example.com/incognito_redirect")

        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user's Browser currently has an active tab open.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        val activeTab = mockTabs.last()
        browser.setActiveTab(activeTab)
        expectThat(browser.activeTab).isEqualTo(activeTab)

        // Say that the load should be intercepted, then load the URL.
        shouldInterceptLoad = true
        browserWrapper.loadUrl(expectedUri)
        coroutineScopeRule.scope.advanceUntilIdle()

        val navigateParams = argumentCaptor<NavigateParams>()
        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore)
        verify(activeTab.navigationController, never()).navigate(eq(expectedUri), any())
        verify(activeTab.navigationController).navigate(eq(redirectUri), navigateParams.capture())
        expectThat(navigateParams.lastValue.isIntentProcessingDisabled).isTrue()

        verify(browser, times(1)).createTab()
    }

    @Test
    fun createAndAttachBrowser_hooksIntoAndroidViewHierarchy() {
        createAndAttachBrowser()
        coroutineScopeRule.scope.advanceUntilIdle()

        verify(browser, never()).setTopView(any())
        verify(browser, never()).setBottomView(any())
        verify(browser).setMinimumSurfaceSize(100, 200)
        verify(fragmentAttacher).invoke(eq(browserFragment), eq(false))

        navigationInfoFlow.value = ActiveTabModel.NavigationInfo(navigationListSize = 1)
        coroutineScopeRule.scope.advanceUntilIdle()

        val topViewCaptor = argumentCaptor<View>()
        val bottomViewCaptor = argumentCaptor<View>()
        verify(browser).setTopView(topViewCaptor.capture())
        verify(browser).setBottomView(bottomViewCaptor.capture())
        expectThat(topViewCaptor.lastValue.layoutParams.height)
            .isEqualTo(context.resources.getDimensionPixelSize(R.dimen.top_toolbar_height))
        expectThat(bottomViewCaptor.lastValue.layoutParams.height)
            .isEqualTo(context.resources.getDimensionPixelSize(R.dimen.bottom_toolbar_height))
    }

    @Test
    fun loadUrl_beforeRestorationCompletes_eventuallyLoadsUrlInActiveTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        createAndAttachBrowser()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Load a URL should open it in a new tab, but restoration hasn't completed yet.
        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = true,
            isViaIntent = false,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(browser.activeTab).isNull()

        // Finish browser restoration.  It should have allowed the blocked URL to load and create
        // a new tab.
        val restoreCallbackCaptor = argumentCaptor<BrowserRestoreCallback>()
        verify(browser).registerBrowserRestoreCallback(restoreCallbackCaptor.capture())
        restoreCallbackCaptor.lastValue.onRestoreCompleted()
        coroutineScopeRule.scope.advanceUntilIdle()

        verify(browser, times(2)).createTab()
        verify(mockTabs.first().navigationController)
            .navigate(eq(Uri.parse(neevaConstants.appURL)), any())
        verify(mockTabs.last().navigationController).navigate(eq(expectedUri), any())
        expectThat(browserWrapper.orderedTabList.value).hasSize(2)
    }

    @Test
    fun loadUrl_withActiveTabAndWithoutInterceptOrNewTabRequirement_loadsUrlInActiveTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user's Browser currently has an active tab open.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        browser.setActiveTab(mockTabs.last())

        // Loading a URL should open it in the existing tab.
        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = false,
            isViaIntent = false,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore)
        verify(browser.activeTab?.navigationController!!).navigate(eq(expectedUri), any())
        verify(browser, times(1)).createTab()
    }

    @Test
    fun loadUrl_withoutActiveTabOrInterceptOrNewTabRequirement_loadsUrlInNewTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        shouldInterceptLoad = false

        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // After restoration, we'll have created a new active tab.  Say there is no active tab for
        // the test by directly nullfying it because WebLayer's browser doesn't allow you to call
        // setActiveTab() with a null Tab.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        val oldActiveTab = browser.activeTab
        activeTab = null

        // Loading a URL without an active tab should create a new one with no parent.
        browserWrapper.loadUrl(
            uri = expectedUri,
            isViaIntent = false,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab.
        expectThat(browser.activeTab).isNotEqualTo(oldActiveTab)
        verify(oldActiveTab?.navigationController!!, never()).navigate(eq(expectedUri), any())
        verify(browser.activeTab?.navigationController!!).navigate(eq(expectedUri), any())
        verify(browser, times(2)).createTab()

        val tab: Tab = mockTabs.last()
        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore + 1)
        val urlCaptor = argumentCaptor<Uri>()
        val navigateParamsCaptor = argumentCaptor<NavigateParams>()
        verify(tab.navigationController).navigate(
            urlCaptor.capture(),
            navigateParamsCaptor.capture()
        )
        expectThat(urlCaptor.lastValue).isEqualTo(expectedUri)

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

        createAndAttachBrowser()
        completeBrowserRestoration()

        val oldActiveTab = browser.activeTab
        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = true,
            isViaIntent = false,
            parentTabId = "parent tab id"
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab and then navigate to the URL.
        expectThat(browser.activeTab).isNotEqualTo(oldActiveTab)
        verify(activeTabModelImpl).onActiveTabChanged(eq(browser.activeTab))
        verify(browser, times(2)).createTab()

        val tab: Tab = mockTabs.last()
        verify(tab.navigationController).navigate(eq(expectedUri), any())

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

        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        val oldActiveTab = browser.activeTab
        browserWrapper.loadUrl(
            uri = expectedUri,
            inNewTab = true,
            isViaIntent = true,
            parentTabId = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab.
        verify(browser, times(2)).createTab()
        expectThat(browser.activeTab).isNotEqualTo(oldActiveTab)
        verify(oldActiveTab?.navigationController!!, never()).navigate(eq(expectedUri), any())
        verify(browser.activeTab?.navigationController!!).navigate(eq(expectedUri), any())

        // Check that the tab's properties were saved.
        val tabInfoList = browserWrapper.orderedTabList.value.filter {
            it.id == browser.activeTab?.guid
        }
        expectThat(tabInfoList).hasSize(1)
        expectThat(tabInfoList[0].data.parentTabId).isNull()
        expectThat(tabInfoList[0].data.openType).isEqualTo(TabInfo.TabOpenType.VIA_INTENT)
    }

    @Test
    fun browserRestoreCallback_withNoTabs_createsNeevaTab() {
        createAndAttachBrowser()
        coroutineScopeRule.scope.advanceUntilIdle()

        val restoreCallbackCaptor = argumentCaptor<BrowserRestoreCallback>()
        verify(browser).registerBrowserRestoreCallback(restoreCallbackCaptor.capture())
        restoreCallbackCaptor.lastValue.onRestoreCompleted()
        coroutineScopeRule.scope.advanceUntilIdle()
    }

    @Test
    fun loadUrl_withLazyTab() {
        val expectedUri = Uri.parse("https://www.example.com")
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        val activeTabBefore = browser.activeTab
        verify(browser, times(1)).createTab()

        // Opening a lazy tab should tell the URL bar to take focus so that the user can see zero
        // query and the other suggestions.
        browserWrapper.openLazyTab()
        verify(urlBarModel).showZeroQuery(focusUrlBar = true, isLazyTab = true)
        urlBarModelStateFlow.value = urlBarModelStateFlow.value.copy(isLazyTab = true)

        // Load a URL without explicitly saying it should be in a new tab.
        browserWrapper.loadUrl(uri = expectedUri)
        coroutineScopeRule.scope.advanceUntilIdle()

        // The Browser should have been asked to create a new tab and then navigate to the URL.
        verify(browser, times(2)).createTab()
        val latestTab = mockTabs.last()

        verify(activeTabBefore?.navigationController!!, never()).navigate(eq(expectedUri), any())
        verify(latestTab.navigationController).navigate(eq(expectedUri), any())

        // Check that the new tab's properties were saved.
        val tabInfoList = browserWrapper.orderedTabList.value.filter { it.id == latestTab.guid }
        expectThat(tabInfoList).hasSize(1)
        expectThat(tabInfoList[0].data.parentTabId).isNull()
        expectThat(tabInfoList[0].data.openType).isEqualTo(TabInfo.TabOpenType.DEFAULT)
    }

    @Test
    fun goBack_whenQueryExists_reshowsSuggestions() {
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        val currentTab = browser.activeTab!!
        val currentNavController = currentTab.navigationController
        Mockito.`when`(activeTabModelImpl.goBack(any(), any())).doAnswer {
            val uri = currentNavController.getNavigationEntryDisplayUri(
                currentNavController.navigationListCurrentIndex
            )
            currentNavController.goBack()

            @Suppress("UNCHECKED_CAST")
            val onNavigatedBack = it.arguments[0] as ((Uri) -> Unit)
            onNavigatedBack.invoke(uri)
        }

        browserWrapper.loadUrl(
            uri = Uri.parse("http://www.firstload.com"),
            inNewTab = false,
            searchQuery = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        browserWrapper.loadUrl(
            uri = Uri.parse("http://www.secondload.com"),
            inNewTab = false,
            searchQuery = "query that triggered loading first example"
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        browserWrapper.loadUrl(
            uri = Uri.parse("http://www.thirdload.com"),
            inNewTab = false,
            searchQuery = "query that triggered loading second example"
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(currentNavController.navigationListCurrentIndex).isEqualTo(3)
        expectThat(currentNavController.navigationListSize).isEqualTo(4)
        expectThat(currentNavController.getNavigationEntryDisplayUri(0))
            .isEqualTo(Uri.parse("https://neeva.com/"))
        expectThat(currentNavController.getNavigationEntryDisplayUri(1))
            .isEqualTo(Uri.parse("http://www.firstload.com"))
        expectThat(currentNavController.getNavigationEntryDisplayUri(2))
            .isEqualTo(Uri.parse("http://www.secondload.com"))
        expectThat(currentNavController.getNavigationEntryDisplayUri(3))
            .isEqualTo(Uri.parse("http://www.thirdload.com"))

        // We should reshow the search suggestions that led to the navigation.
        browserWrapper.goBack()
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(urlBarModel).showZeroQuery(false)
        verify(urlBarModel).replaceLocationBarText("query that triggered loading second example")
        expectThat(currentNavController.navigationListCurrentIndex).isEqualTo(2)
        expectThat(currentNavController.navigationListSize).isEqualTo(4)

        browserWrapper.goBack()
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(urlBarModel, times(2)).showZeroQuery(false)
        verify(urlBarModel).replaceLocationBarText("query that triggered loading first example")
        expectThat(currentNavController.navigationListCurrentIndex).isEqualTo(1)
        expectThat(currentNavController.navigationListSize).isEqualTo(4)

        // Navigating forward should overwrite the saved query data.
        browserWrapper.loadUrl(
            uri = Uri.parse("http://www.fourthload.com"),
            inNewTab = false,
            searchQuery = null
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(currentNavController.getNavigationEntryDisplayUri(0))
            .isEqualTo(Uri.parse("https://neeva.com/"))
        expectThat(currentNavController.getNavigationEntryDisplayUri(1))
            .isEqualTo(Uri.parse("http://www.firstload.com"))
        expectThat(currentNavController.getNavigationEntryDisplayUri(2))
            .isEqualTo(Uri.parse("http://www.fourthload.com"))
        expectThat(currentNavController.navigationListCurrentIndex).isEqualTo(2)
        expectThat(currentNavController.navigationListSize).isEqualTo(3)

        // We shouldn't have tried to reshow the search results because the queries are gone.
        verify(urlBarModel, times(2)).showZeroQuery(false)
        browserWrapper.goBack()
        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(currentNavController.navigationListCurrentIndex).isEqualTo(1)
        expectThat(currentNavController.navigationListSize).isEqualTo(3)
        verify(urlBarModel, times(2)).showZeroQuery(false)
    }

    @Test
    fun lazyTab_tracksEditingState() {
        val expectedUri = Uri.parse("https://www.example.com")
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user has an active tab.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        browser.setActiveTab(mockTabs.last())

        // Opening a lazy tab should tell the URL bar to take focus so that the user can see zero
        // query and the other suggestions.
        browserWrapper.openLazyTab()
        verify(urlBarModel).showZeroQuery(focusUrlBar = true, isLazyTab = true)
        urlBarModelStateFlow.value = urlBarModelStateFlow.value.copy(
            isEditing = true,
            isLazyTab = true
        )

        // Say that the URL bar lost focus.
        coroutineScopeRule.scope.advanceUntilIdle()
        urlBarModelStateFlow.value = urlBarModelStateFlow.value.copy(
            isEditing = false,
            isLazyTab = false
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // Load a URL.  Because the lazy tab state was lost, the tab should be opened up in the
        // existing tab.
        browserWrapper.loadUrl(uri = expectedUri)
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore)
        verify(browser.activeTab!!.navigationController).navigate(eq(expectedUri), any())
        verify(browser, times(1)).createTab()
    }

    @Test
    fun startClosingTab_afterCancel_restoresTab() {
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user has an active tab.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        val tab = mockTabs.last()
        browser.setActiveTab(tab)

        // Start closing the active tab.
        browserWrapper.startClosingTab(tab.guid)
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(numTabsBefore)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isTrue()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isFalse()

        // Canceling the tab closure should bring the tab back.
        browserWrapper.cancelClosingTab(tab.guid)
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(numTabsBefore)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isFalse()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isFalse()
    }

    @Test
    fun startClosingTab_thenCloseTab() {
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user has an active tab.
        val numTabsBefore = browserWrapper.orderedTabList.value.size
        val tab = mockTabs.last()
        browser.setActiveTab(tab)

        // Start closing the active tab.
        browserWrapper.startClosingTab(tab.guid)
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(numTabsBefore)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isTrue()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isFalse()

        // Actually close it.
        browserWrapper.closeTab(tab.guid)

        // Confirm that the Tab was told to close.  The callback saying it was removed should fire.
        verify(tab).dispatchBeforeUnloadAndClose()

        // Confirm that everything is gone.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(0)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isTrue()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isTrue()
    }

    @Test
    fun closeTab() {
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.scope.advanceUntilIdle()

        // Say that the user has an active tab.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(1)
        val tab = mockTabs.last()
        browser.setActiveTab(tab)

        // Close the tab.
        browserWrapper.closeTab(tab.guid)

        // Confirm that the Tab was told to close.  The callback saying it was removed should fire.
        verify(tab).dispatchBeforeUnloadAndClose()

        // Confirm that everything is gone.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(0)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isTrue()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isTrue()
    }
}
