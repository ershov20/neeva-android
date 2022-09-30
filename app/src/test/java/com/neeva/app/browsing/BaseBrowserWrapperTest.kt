// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.contentfilter.ScriptInjectionManager
import com.neeva.app.contentfilter.TrackersAllowList
import com.neeva.app.createMockNavigationController
import com.neeva.app.history.HistoryManager
import com.neeva.app.neevascope.NeevaScopeModel
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPrefFolder.App.AutomaticallyArchiveTabs
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import com.neeva.app.storage.entities.TabData
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import java.util.EnumSet
import java.util.concurrent.TimeUnit
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
import strikt.assertions.containsExactly
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
    private lateinit var browserWrapper: BaseBrowserWrapper
    private lateinit var context: Context
    private lateinit var currentTimeProvider: () -> Long
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var profile: Profile
    private lateinit var settingsDataModel: SettingsDataModel
    private lateinit var sharedPreferencesModel: SharedPreferencesModel
    private lateinit var tabList: TabList
    private lateinit var urlBarModel: URLBarModelImpl

    // Default mocks automatically initialized via Mockito.mockitoSession().initMocks().
    @Mock private lateinit var activityCallbackProvider: ActivityCallbackProvider
    @Mock private lateinit var contentFilterManager: ContentFilterManager
    @Mock private lateinit var contentFilterModel: ContentFilterModel
    @Mock private lateinit var cookieManager: CookieManager
    @Mock private lateinit var domainProvider: DomainProvider
    @Mock private lateinit var faviconCache: FaviconCache
    @Mock private lateinit var findInPageModel: FindInPageModelImpl
    @Mock private lateinit var fragmentAttacher: (fragment: Fragment, isIncognito: Boolean) -> Unit
    @Mock private lateinit var historyManager: HistoryManager
    @Mock private lateinit var spaceStore: SpaceStore
    @Mock private lateinit var suggestionsModel: SuggestionsModel
    @Mock private lateinit var neevaScopeModel: NeevaScopeModel
    @Mock private lateinit var tabScreenshotManager: TabScreenshotManager
    @Mock private lateinit var scriptInjectionManager: ScriptInjectionManager
    @Mock private lateinit var popupModel: PopupModel
    @Mock private lateinit var neevaUser: NeevaUser
    @Mock private lateinit var trackersAllowList: TrackersAllowList

    private lateinit var navigationInfoFlow: MutableStateFlow<ActiveTabModel.NavigationInfo>
    private lateinit var urlBarModelStateFlow: MutableStateFlow<URLBarModelState>
    private lateinit var mockTabs: MutableList<Tab>

    private var activeTab: Tab? = null
    private var shouldInterceptLoad: Boolean = false
    private var wasBlankTabCreated: Boolean = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()

        neevaConstants = NeevaConstants()
        sharedPreferencesModel = SharedPreferencesModel(context)
        settingsDataModel = SettingsDataModel(sharedPreferencesModel)
        currentTimeProvider = mock {
            on { invoke() } doReturn TimeUnit.DAYS.toMillis(100)
        }

        // Use an incognito tab list because it's implemented as a straightforward in-memory list
        // and doesn't need a HistoryDatabase to work.
        tabList = IncognitoTabList()

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

        contentFilterModel = mock {
            on { trackingDataFlow } doReturn MutableStateFlow(null)
            on { enableTrackingProtection } doReturn mutableStateOf(true)
            on { cookieNoticeBlockedFlow } doReturn MutableStateFlow(false)
            on { cookieCuttingPreferences } doReturn mutableStateOf(
                EnumSet.noneOf(ContentFilterModel.CookieNoticeCookies::class.java)
            )
            on { trackersAllowList } doReturn trackersAllowList
        }

        scriptInjectionManager = mock()

        browserWrapper = object : BaseBrowserWrapper(
            isIncognito = false,
            appContext = context,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            popupModel = popupModel,
            activityCallbackProvider = activityCallbackProvider,
            neevaUser = neevaUser,
            neevaConstants = neevaConstants,
            domainProvider = domainProvider,
            suggestionsModel = suggestionsModel,
            neevaScopeModel = neevaScopeModel,
            faviconCache = faviconCache,
            spaceStore = spaceStore,
            tabList = tabList,
            _activeTabModelImpl = activeTabModelImpl,
            _urlBarModel = urlBarModel,
            _findInPageModel = findInPageModel,
            historyManager = historyManager,
            tabScreenshotManager = tabScreenshotManager,
            scriptInjectionManager = scriptInjectionManager,
            settingsDataModel = settingsDataModel,
            sharedPreferencesModel = sharedPreferencesModel,
            contentFilterModel = contentFilterModel,
            getCurrentTime = currentTimeProvider,
            clientLogger = null
        ) {
            override fun createBrowserFragment(): Fragment =
                this@BaseBrowserWrapperTest.browserFragment

            override fun getBrowserFromFragment(fragment: Fragment): Browser {
                return this@BaseBrowserWrapperTest.browser
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
        expectThat(tabInfoList[0].data.parentTabId).isEqualTo(activeTabBefore.guid)
        expectThat(tabInfoList[0].data.openType).isEqualTo(TabInfo.TabOpenType.CHILD_TAB)
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

        // Load a URL.
        browserWrapper.loadUrl(uri = expectedUri)
        coroutineScopeRule.scope.advanceUntilIdle()

        // Because no existing tab was found with the same URL, it should create a new tab.
        expectThat(browserWrapper.orderedTabList.value).hasSize(numTabsBefore + 1)
        verify(browser.activeTab!!.navigationController).navigate(eq(expectedUri), any())
        verify(browser, times(2)).createTab()
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
        coroutineScopeRule.advanceUntilIdle()

        // Confirm that the Tab was told to close.  The callback saying it was removed should fire.
        verify(tab).dispatchBeforeUnloadAndClose()

        // Confirm that everything is gone.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(0)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isTrue()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isTrue()
    }

    @Test
    fun closeTab_withValidUrl_addsToArchive() {
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.advanceUntilIdle()

        // Say that the user has an active tab.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(1)
        val tab = mockTabs.find { browserWrapper.orderedTabList.value[0].id == it.guid }!!
        browser.setActiveTab(tab)

        // Navigate the tab somewhere.  We have to manually update the TabList because our mock Tab
        // doesn't fire the WebLayer callbacks.
        browserWrapper.loadUrl(Uri.parse("https://www.example.com"), inNewTab = false)
        coroutineScopeRule.advanceUntilIdle()
        tabList.updateUrl(tab.guid, Uri.parse("https://www.example.com"))

        // Close the tab.
        browserWrapper.closeTab(tab.guid)
        coroutineScopeRule.advanceUntilIdle()

        // Confirm that the Tab was told to close.  The callback saying it was removed should fire.
        verify(tab).dispatchBeforeUnloadAndClose()

        // Confirm that everything is gone.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(0)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isTrue()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isTrue()

        // Confirm that the tab was added to the tab archive.
        val tabDataCaptor = argumentCaptor<TabData>()
        verify(historyManager).addArchivedTab(tabDataCaptor.capture())
    }

    @Test
    fun closeTab_withoutValidUrl_doesNotAddToArchive() {
        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.advanceUntilIdle()

        // Say that the user has an active tab, but don't update the TabList to say that it has a
        // non-null URL.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(1)
        val tab = mockTabs.find { browserWrapper.orderedTabList.value[0].id == it.guid }!!
        browser.setActiveTab(tab)

        // Close the tab.
        browserWrapper.closeTab(tab.guid)
        coroutineScopeRule.advanceUntilIdle()

        // Confirm that the Tab was told to close.  The callback saying it was removed should fire.
        verify(tab).dispatchBeforeUnloadAndClose()

        // Confirm that everything is gone.
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(0)
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = true)).isTrue()
        expectThat(browserWrapper.hasNoTabs(ignoreClosingTabs = false)).isTrue()

        // Confirm that the tab was NOT added to the tab archive.
        val tabDataCaptor = argumentCaptor<TabData>()
        verify(historyManager, never()).addArchivedTab(tabDataCaptor.capture())
    }

    @Test
    fun closeInactiveTabs_withAutomatedTabManagementEnabled_closesOldTabs() {
        AutomaticallyArchiveTabs.set(sharedPreferencesModel, ArchiveAfterOption.AFTER_7_DAYS)

        createAndAttachBrowser()
        completeBrowserRestoration()
        coroutineScopeRule.advanceUntilIdle()
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(1)

        // Add two new tabs.
        browserWrapper.loadUrl(
            uri = Uri.parse("https://www.example.com"),
            inNewTab = true
        )
        browserWrapper.loadUrl(
            uri = Uri.parse("https://www.example2.com"),
            inNewTab = true
        )
        browserWrapper.loadUrl(
            uri = Uri.parse("https://www.example3.com"),
            inNewTab = true
        )
        coroutineScopeRule.advanceUntilIdle()
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(4)
        expectThat(mockTabs).hasSize(4)

        // Set the timestamps on all the URLs.
        tabList.updateTimestamp(mockTabs[0], currentTimeProvider() - TimeUnit.DAYS.toMillis(10))
        tabList.updateTimestamp(mockTabs[1], currentTimeProvider() - TimeUnit.DAYS.toMillis(5))
        tabList.updateTimestamp(mockTabs[2], currentTimeProvider() - TimeUnit.DAYS.toMillis(8))
        tabList.updateTimestamp(mockTabs[3], currentTimeProvider() - TimeUnit.DAYS.toMillis(30))
        val ids = mockTabs.map { it.guid }
        coroutineScopeRule.advanceUntilIdle()

        // Closing inactive tabs should close old tabs that aren't currently selected.
        browserWrapper.closeInactiveTabs()
        coroutineScopeRule.advanceUntilIdle()
        expectThat(browserWrapper.orderedTabList.value.size).isEqualTo(2)
        expectThat(browserWrapper.orderedTabList.value.map { it.id })
            .containsExactly(ids[1], ids[3])
    }
}
