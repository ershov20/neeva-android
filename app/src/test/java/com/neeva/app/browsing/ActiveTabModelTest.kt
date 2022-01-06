package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.BaseTest
import com.neeva.app.publicsuffixlist.DomainProvider
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ActiveTabModelTest: BaseTest() {
    @Mock lateinit var tabCreator: TabCreator

    private lateinit var domainProvider: DomainProvider

    private lateinit var model: ActiveTabModel

    private lateinit var mainTab: MockTabHarness

    @Before
    override fun setUp() {
        super.setUp()

        domainProvider = mock {
            on { getRegisteredDomain(eq(Uri.parse("https://www.site.com/1"))) } doReturn "site.com"
            on { getRegisteredDomain(eq(Uri.parse("http://www.othersite.com/2"))) } doReturn "othersite.com"
        }

        model = ActiveTabModel(tabCreator, domainProvider)

        mainTab = MockTabHarness(
            currentTitle = "Title #1",
            currentUri = Uri.parse("https://www.site.com/1"),
            canGoBack = true,
            canGoForward = false
        )
    }

    @Test
    fun onActiveTabChanged_updatesValuesAfterTabSwitches() {
        expectThat(model.activeTabFlow.value).isNull()
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        expectThat(model.activeTabFlow.value).isEqualTo(mainTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(mainTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)
        expectThat(model.displayedDomain.value).isEqualTo("site.com")
        expectThat(model.showLock.value).isTrue()
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(1)

        // Set the second tab as active and confirm the flows were updated.
        val secondTab = MockTabHarness(
            currentTitle = "Title #2",
            currentUri = Uri.parse("http://www.othersite.com/2"),
            canGoBack = false,
            canGoForward = true
        )
        model.onActiveTabChanged(secondTab.tab)
        expectThat(model.activeTabFlow.value).isEqualTo(secondTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(secondTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(secondTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(secondTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(secondTab.canGoForward)
        expectThat(model.displayedDomain.value).isEqualTo("othersite.com")
        expectThat(model.showLock.value).isFalse()
        expectThat(secondTab.tabCallbacks.size).isEqualTo(1)
        expectThat(secondTab.navigationCallbacks.size).isEqualTo(1)

        // The first tab's callbacks should have been unregistered.
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(0)
    }

    @Test
    fun onActiveTabChanged_updatesValuesAfterNullActiveTab() {
        expectThat(model.activeTabFlow.value).isNull()
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        expectThat(model.activeTabFlow.value).isEqualTo(mainTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(mainTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)
        expectThat(model.displayedDomain.value).isEqualTo("site.com")
        expectThat(model.showLock.value).isTrue()
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(1)

        // Set the second tab as active and confirm the flows were updated.
        model.onActiveTabChanged(null)
        expectThat(model.activeTabFlow.value).isEqualTo(null)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.displayedDomain.value).isEqualTo("")
        expectThat(model.showLock.value).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(false)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(false)

        // The first tab's callbacks should have been unregistered.
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(0)
    }

    @Test
    fun onActiveTabChanged_monitorsChangesToTab() {
        expectThat(model.activeTabFlow.value).isNull()
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(Uri.EMPTY)
        expectThat(model.titleFlow.value).isEqualTo("")
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        expectThat(model.activeTabFlow.value).isEqualTo(mainTab.tab)
        expectThat(model.progressFlow.value).isEqualTo(100)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.titleFlow.value).isEqualTo(mainTab.currentTitle)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isEqualTo(mainTab.canGoBack)
        expectThat(model.navigationInfoFlow.value.canGoForward).isEqualTo(mainTab.canGoForward)
        expectThat(model.displayedDomain.value).isEqualTo("site.com")
        expectThat(model.showLock.value).isTrue()
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.navigationCallbacks.size).isEqualTo(1)

        // Check that the title gets updated.
        mainTab.currentTitle = "New title"
        mainTab.tabCallbacks.first().onTitleUpdated(mainTab.currentTitle ?: "")
        expectThat(model.titleFlow.value).isEqualTo("New title")

        // Check that the Uri gets updated.
        mainTab.currentUri = Uri.parse("http://www.othersite.com/2")
        mainTab.tabCallbacks.first().onVisibleUriChanged(mainTab.currentUri ?: Uri.EMPTY)
        expectThat(model.urlFlow.value).isEqualTo(mainTab.currentUri)
        expectThat(model.displayedDomain.value).isEqualTo("othersite.com")
        expectThat(model.showLock.value).isFalse()

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
    fun loadUrl_withNoActiveTab_createsNewTab() {
        val uri = Uri.parse("https://www.reddit.com")
        model.loadUrl(uri = uri, newTab = false)
        verify(tabCreator, times(1)).createTabWithUri(eq(uri), eq(null))
    }

    @Test
    fun loadUrl_withActiveTabInNewTab_loadsInNewTab() {
        model.onActiveTabChanged(mainTab.tab)

        val uri = Uri.parse("https://www.reddit.com")
        model.loadUrl(uri = uri, newTab = true)
        verify(tabCreator, times(1)).createTabWithUri(eq(uri), eq(null))
        verify(mainTab.navigationController, never()).navigate(any(), any())
    }

    @Test
    fun loadUrl_withActiveTabInSameTab_loadsInSameTab() {
        model.onActiveTabChanged(mainTab.tab)

        val uri = Uri.parse("https://www.reddit.com")
        model.loadUrl(uri = uri, newTab = true)
        verify(tabCreator, never()).createTabWithUri(any(), any())
        verify(mainTab.navigationController, never()).navigate(eq(uri), any())
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
