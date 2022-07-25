package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.ActiveTabModel.DisplayMode
import com.neeva.app.browsing.TabInfo.TabOpenType
import com.neeva.app.storage.TabScreenshotManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ActiveTabModelImplTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var onCloseTab: (String) -> Unit
    @Mock private lateinit var onShowSearchResults: (Uri) -> Unit
    @Mock private lateinit var tabScreenshotManager: TabScreenshotManager

    private lateinit var model: ActiveTabModelImpl
    private lateinit var mainTab: MockTabHarness
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var neevaHomepageTab: MockTabHarness
    private lateinit var neevaSearchTab: MockTabHarness
    private lateinit var neevaSpacesTab: MockTabHarness
    private lateinit var tabList: TabList

    @Before
    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()
        tabList = TabList()

        model = ActiveTabModelImpl(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = Dispatchers(
                StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
                StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            ),
            neevaConstants = neevaConstants,
            tabScreenshotManager = tabScreenshotManager,
            tabList = tabList
        )

        mainTab = MockTabHarness(
            currentTitle = "Title #1",
            currentUri = Uri.parse("https://www.site.com/1"),
            canGoBack = true,
            canGoForward = true
        )
        neevaHomepageTab = MockTabHarness(
            currentTitle = "Neeva homepage",
            currentUri = Uri.parse(neevaConstants.appURL),
            canGoBack = false,
            canGoForward = false
        )
        neevaSearchTab = MockTabHarness(
            currentTitle = "Neeva Search",
            currentUri = Uri.parse("https://neeva.com/search?q=query"),
            canGoBack = true,
            canGoForward = false
        )
        neevaSpacesTab = MockTabHarness(
            currentTitle = "Neeva Spaces",
            currentUri = Uri.parse("https://neeva.com/spaces"),
            canGoBack = true,
            canGoForward = false
        )
    }

    @Test
    fun onActiveTabChanged_updatesValuesAfterTabSwitches() {
        expectThat(model.activeTab).isNull()
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        expectThat(model.activeTab).isEqualTo(mainTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(mainTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("www.site.com")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(1)

        // Set the second tab as active and confirm the flows were updated.
        val secondTab = MockTabHarness(
            currentTitle = "Title #2",
            currentUri = Uri.parse("http://news.othersite.com/2"),
            canGoBack = false,
            canGoForward = true
        )
        model.onActiveTabChanged(secondTab.tab)
        expectThat(model.activeTab).isEqualTo(secondTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(secondTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(secondTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(secondTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(secondTab.canGoForward)
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("news.othersite.com")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        expectThat(secondTab.tabCallbacks.size).isEqualTo(1)
        expectThat(secondTab.navigationCallbacks.size).isEqualTo(1)

        // The first tab's callbacks should have been unregistered.
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(0)
    }

    @Test
    fun onActiveTabChanged_updatesValuesAfterNullActiveTab() {
        expectThat(model.activeTab).isNull()
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        expectThat(model.activeTab).isEqualTo(mainTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(mainTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("www.site.com")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(1)

        // Set the second tab as active and confirm the flows were updated.
        model.onActiveTabChanged(null)
        expectThat(model.activeTab).isEqualTo(null)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(false)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(false)

        // The first tab's callbacks should have been unregistered.
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(0)
    }

    @Test
    fun mode_onNeevaHomepage_showsPlaceholder() {
        model.onActiveTabChanged(neevaHomepageTab.tab)
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(0)
        expectThat(model.activeTab).isEqualTo(neevaHomepageTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(neevaHomepageTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(neevaHomepageTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward)
            .isEqualTo(neevaHomepageTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward)
            .isEqualTo(neevaHomepageTab.canGoForward)
        expectThat(model.displayedInfoFlow.value.displayedText).isEmpty()
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.PLACEHOLDER)
        expectThat(neevaHomepageTab.tabCallbacks.size).isEqualTo(1)
        expectThat(neevaHomepageTab.navigationCallbacks.size).isEqualTo(1)
    }

    @Test
    fun displayedText_showsDomainAndQuery() {
        model.onActiveTabChanged(neevaSearchTab.tab)
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(0)
        expectThat(model.activeTab).isEqualTo(neevaSearchTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(neevaSearchTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(neevaSearchTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward)
            .isEqualTo(neevaSearchTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward)
            .isEqualTo(neevaSearchTab.canGoForward)
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("query")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.QUERY)
        expectThat(neevaSearchTab.tabCallbacks.size).isEqualTo(1)
        expectThat(neevaSearchTab.navigationCallbacks.size).isEqualTo(1)

        model.onActiveTabChanged(neevaSpacesTab.tab)
        expectThat(neevaSearchTab.tabCallbacks.size).isEqualTo(0)
        expectThat(neevaSearchTab.navigationCallbacks.size).isEqualTo(0)
        expectThat(model.activeTab).isEqualTo(neevaSpacesTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(neevaSpacesTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(neevaSpacesTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(neevaSpacesTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward)
            .isEqualTo(neevaSpacesTab.canGoForward)
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("neeva.com")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        expectThat(neevaSpacesTab.tabCallbacks.size).isEqualTo(1)
        expectThat(neevaSpacesTab.navigationCallbacks.size).isEqualTo(1)
    }

    @Test
    fun onActiveTabChanged_monitorsChangesToTab() {
        expectThat(model.activeTab).isNull()
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        expectThat(model.activeTab).isEqualTo(mainTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(mainTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("www.site.com")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(1)

        // Check that the title gets updated.
        mainTab.currentTitle = "New title"
        mainTab.tabCallbacks.first().onTitleUpdated(mainTab.currentTitle ?: "")
        expectThat(model.titleFlow.value).isEqualTo("New title")

        // Check that the Uri gets updated.
        mainTab.currentUri = Uri.parse("http://news.othersite.com/2")
        mainTab.tabCallbacks.first().onVisibleUriChanged(mainTab.currentUri ?: Uri.EMPTY)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("news.othersite.com")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)

        // Check that navigations are kept in sync.
        mainTab.canGoBack = false
        mainTab.canGoForward = true
        mainTab.navigationCallbacks.first().onNavigationStarted(mock())
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)

        mainTab.canGoBack = true
        mainTab.canGoForward = false
        mainTab.navigationCallbacks.first().onNavigationStarted(mock())
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)

        mainTab.navigationCallbacks.first().onLoadProgressChanged(0.429)
        expectThat(model.progressFlow.value).isEqualTo(43)
        verify(tabScreenshotManager, never()).captureAndSaveScreenshot(any(), any())

        mainTab.navigationCallbacks.first().onLoadProgressChanged(1.0)
        expectThat(model.progressFlow.value).isEqualTo(100)
        verify(tabScreenshotManager).captureAndSaveScreenshot(any(), any())
    }

    @Test
    fun reload_withActiveTab_reloadsTab() {
        model.onActiveTabChanged(mainTab.tab)
        model.reload()
        verify(mainTab.navigationController, times(1)).reload()
    }

    @Test
    fun goBack() {
        // Set the tab.
        val harness = MockTabHarness(
            currentTitle = "Title",
            currentUri = Uri.parse("https://www.site.com/"),
            canGoBack = false,
            canGoForward = false
        )
        model.onActiveTabChanged(harness.tab)

        model.goBack(onNavigatedBack = onShowSearchResults, onCloseTab = onCloseTab)
        verify(harness.navigationController, never()).goBack()
        verify(onCloseTab, never()).invoke(any())

        // Say that the user navigated somewhere and we can now go backward.
        harness.canGoBack = true
        harness.navigationCallbacks.first().onNavigationCompleted(mock())

        model.goBack(onShowSearchResults, onCloseTab)
        verify(harness.navigationController, times(1)).goBack()
    }

    @Test
    fun goBack_forChildTab() {
        // Set the tab.
        val parentTabHarness = MockTabHarness(
            currentTitle = "Parent",
            currentUri = Uri.parse("https://www.techmeme.com/"),
            canGoBack = false,
            canGoForward = false,
            tabId = "parent tab"
        )
        val childTabHarness = MockTabHarness(
            currentTitle = "Child",
            currentUri = Uri.parse("https://www.sitelinkedfromtechmeme.com/"),
            canGoBack = false,
            canGoForward = false,
            tabId = "child tab",
            parentTabId = parentTabHarness.tabId
        )

        model.onActiveTabChanged(childTabHarness.tab)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()

        // Hit "back" on the child tab.
        model.goBack(onNavigatedBack = onShowSearchResults, onCloseTab = onCloseTab)

        // Even though there are no navigations on this tab, because it is a child we should have
        // closed it.
        verify(childTabHarness.navigationController, never()).goBack()
        verify(onCloseTab).invoke(any())
    }

    @Test
    fun goBack_forChildTabAfterParentClosed_doesNothing() {
        // Set the tab.
        val parentTabHarness = MockTabHarness(
            currentTitle = "Parent",
            currentUri = Uri.parse("https://www.techmeme.com/"),
            canGoBack = false,
            canGoForward = false,
            tabId = "parent tab"
        )
        val childTabHarness = MockTabHarness(
            currentTitle = "Child",
            currentUri = Uri.parse("https://www.sitelinkedfromtechmeme.com/"),
            canGoBack = false,
            canGoForward = false,
            tabId = "child tab",
            parentTabId = parentTabHarness.tabId
        )

        model.onActiveTabChanged(childTabHarness.tab)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()

        // Say that the parent tab has been removed.
        tabList.remove(parentTabHarness.tabId)
        model.onTabRemoved(parentTabHarness.tab.guid)

        // We shouldn't be able to hit back, now.
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()

        // Hit "back" on the child tab.
        model.goBack(onNavigatedBack = onShowSearchResults, onCloseTab = onCloseTab)

        // Nothing should happen.
        verify(childTabHarness.navigationController, never()).goBack()
        verify(onCloseTab, never()).invoke(any())
    }

    @Test
    fun goForward() {
        // Set the tab.
        val harness = MockTabHarness(
            currentTitle = "Title",
            currentUri = Uri.parse("https://www.site.com/"),
            canGoBack = false,
            canGoForward = false
        )
        model.onActiveTabChanged(harness.tab)

        model.goForward()
        verify(harness.navigationController, never()).goForward()

        // Say that the user navigated somewhere and we can now go forward.
        harness.canGoForward = true
        harness.navigationCallbacks.first().onNavigationCompleted(mock())

        model.goForward()
        verify(harness.navigationController, times(1)).goForward()
    }

    private var nextMockTabId: Int = 0
    inner class MockTabHarness(
        var currentTitle: String? = null,
        var currentUri: Uri? = null,
        var canGoBack: Boolean = false,
        var canGoForward: Boolean = false,
        val tabId: String = (nextMockTabId++).toString(),
        val parentTabId: String? = null
    ) {
        val tabCallbacks = mutableSetOf<TabCallback>()
        val navigationCallbacks = mutableSetOf<NavigationCallback>()

        val navigationController = mock<NavigationController> {
            on { registerNavigationCallback(any()) } doAnswer {
                val callback = it.getArgument(0) as NavigationCallback
                expectThat(navigationCallbacks.contains(callback)).isFalse()
                navigationCallbacks.add(callback)
                Unit
            }

            on { unregisterNavigationCallback(any()) } doAnswer {
                val callback = it.getArgument(0) as NavigationCallback
                expectThat(navigationCallbacks.contains(callback)).isTrue()
                navigationCallbacks.remove(callback)
                Unit
            }

            // Set up the test so that it always returns the title and URL that we set.
            on { navigationListSize } doReturn 1
            on { navigationListCurrentIndex } doReturn 0
            on { getNavigationEntryTitle(0) } doAnswer { currentTitle }
            on { getNavigationEntryDisplayUri(0) } doAnswer { currentUri }
            on { canGoBack() } doAnswer { canGoBack }
            on { canGoForward() } doAnswer { canGoForward }
        }

        val tab = mock<Tab> {
            on { guid } doReturn tabId

            on { navigationController } doReturn navigationController

            on { registerTabCallback(any()) } doAnswer {
                val callback = it.getArgument(0) as TabCallback
                expectThat(tabCallbacks.contains(callback)).isFalse()
                tabCallbacks.add(callback)
                Unit
            }

            on { unregisterTabCallback(any()) } doAnswer {
                val callback = it.getArgument(0) as TabCallback
                expectThat(tabCallbacks.contains(callback)).isTrue()
                tabCallbacks.remove(callback)
                Unit
            }
        }

        init {
            tabList.add(tab)
            tabList.updateParentInfo(
                tab = tab,
                parentTabId = parentTabId,
                tabOpenType = parentTabId?.let { TabOpenType.CHILD_TAB } ?: TabOpenType.DEFAULT
            )
        }
    }
}
