package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.BaseTest
import com.neeva.app.Dispatchers
import kotlinx.coroutines.CoroutineScope
import org.chromium.weblayer.NavigateParams
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ActiveTabModelImplTest : BaseTest() {
    private lateinit var model: ActiveTabModelImpl

    private lateinit var mainTab: MockTabHarness
    private lateinit var neevaSearchTab: MockTabHarness
    private lateinit var neevaSpacesTab: MockTabHarness

    @Before
    override fun setUp() {
        super.setUp()

        model = ActiveTabModelImpl(
            coroutineScope = mock<CoroutineScope>(),
            dispatchers = mock<Dispatchers>()
        )

        mainTab = MockTabHarness(
            currentTitle = "Title #1",
            currentUri = Uri.parse("https://www.site.com/1"),
            canGoBack = true,
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
        expectThat(model.displayedText.value).isEqualTo("www.site.com")
        expectThat(model.isShowingQuery.value).isFalse()
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
        expectThat(model.displayedText.value).isEqualTo("news.othersite.com")
        expectThat(model.isShowingQuery.value).isFalse()
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
        expectThat(model.displayedText.value).isEqualTo("www.site.com")
        expectThat(model.isShowingQuery.value).isFalse()
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(1)

        // Set the second tab as active and confirm the flows were updated.
        model.onActiveTabChanged(null)
        expectThat(model.activeTab).isEqualTo(null)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.displayedText.value).isEqualTo("")
        expectThat(model.isShowingQuery.value).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(false)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(false)

        // The first tab's callbacks should have been unregistered.
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(0)
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
        expectThat(model.displayedText.value).isEqualTo("query")
        expectThat(model.isShowingQuery.value).isTrue()
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
        expectThat(model.displayedText.value).isEqualTo("neeva.com")
        expectThat(model.isShowingQuery.value).isFalse()
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
        expectThat(model.displayedText.value).isEqualTo("www.site.com")
        expectThat(model.isShowingQuery.value).isFalse()
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
        expectThat(model.displayedText.value).isEqualTo("news.othersite.com")
        expectThat(model.isShowingQuery.value).isFalse()

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

        mainTab.navigationCallbacks.first().onLoadProgressChanged(42.9)
        expectThat(model.progressFlow.value).isEqualTo(4290)
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

        model.goBack()
        verify(harness.navigationController, never()).goBack()

        // Say that the user navigated somewhere and we can now go backward.
        harness.canGoBack = true
        harness.navigationCallbacks.first().onNavigationCompleted(mock())

        model.goBack()
        verify(harness.navigationController, times(1)).goBack()
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

    @Test
    fun loadUrl() {
        // Set the tab.
        val harness = MockTabHarness(
            currentTitle = "Title",
            currentUri = Uri.parse("https://www.site.com/"),
            canGoBack = false,
            canGoForward = false
        )
        model.onActiveTabChanged(harness.tab)

        val uri = Uri.parse("https://www.reddit.com")
        model.loadUrlInActiveTab(uri)

        val navigateParamsCaptor = argumentCaptor<NavigateParams>()
        verify(harness.navigationController).navigate(eq(uri), navigateParamsCaptor.capture())
        expectThat(navigateParamsCaptor.lastValue.isIntentProcessingDisabled).isTrue()
    }

    class MockTabHarness(
        var currentTitle: String? = null,
        var currentUri: Uri? = null,
        var canGoBack: Boolean = false,
        var canGoForward: Boolean = false
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
    }
}
