// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.MockNavigationController
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.ActiveTabModel.DisplayMode
import com.neeva.app.browsing.TabInfo.TabOpenType
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ActiveTabModelImplTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var spaceStore: SpaceStore
    @Mock private lateinit var tabScreenshotManager: TabScreenshotManager

    private lateinit var model: ActiveTabModelImpl
    private lateinit var mainTab: MockHarness
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var neevaHomepageTab: MockHarness
    private lateinit var neevaSearchTab: MockHarness
    private lateinit var neevaSpacesTab: MockHarness
    private lateinit var tabList: TabList

    @Before
    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()
        tabList = IncognitoTabList()

        model = ActiveTabModelImpl(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = Dispatchers(
                StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
                StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            ),
            neevaConstants = neevaConstants,
            tabScreenshotManager = tabScreenshotManager,
            tabList = tabList,
            spaceStore = spaceStore
        )

        mainTab = MockHarness(
            navigations = listOf(
                Uri.parse("https://www.site.com/0"),
                Uri.parse("https://www.site.com/1"),
            )
        )
        neevaHomepageTab = MockHarness(
            navigations = listOf(
                Uri.parse(neevaConstants.appURL)
            )
        )
        neevaSearchTab = MockHarness(
            navigations = listOf(
                Uri.parse("https://neeva.com/search?q=query")
            )
        )
        neevaSpacesTab = MockHarness(
            navigations = listOf(
                Uri.parse("https://neeva.com/landing"),
                Uri.parse("https://neeva.com/spaces")
            )
        )
    }

    @Test
    fun onActiveTabChanged_updatesValuesAfterTabSwitches() {
        model.apply {
            expectThat(activeTab).isNull()
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(Uri.EMPTY)
            expectThat(titleFlow.value).isEqualTo("")
            expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
        }

        // Set the first tab as active.
        model.onActiveTabChanged(mainTab.tab)

        // Confirm that the flows all updated.
        model.apply {
            expectThat(activeTab).isEqualTo(mainTab.tab)
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(mainTab.tab.currentDisplayUrl)
            expectThat(titleFlow.value).isEqualTo(mainTab.tab.currentDisplayTitle)
            expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            expectThat(displayedInfoFlow.value.displayedText).isEqualTo("www.site.com")
            expectThat(displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        }
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.mockNavigationController.callbacks.size).isEqualTo(1)

        // Set the second tab as active and confirm the flows were updated.
        val secondTab = MockHarness(
            navigations = listOf(
                Uri.parse("http://news.othersite.com/1"),
                Uri.parse("http://news.othersite.com/2")
            )
        )
        secondTab.mockNavigationController.controller.goBack()
        model.onActiveTabChanged(secondTab.tab)

        model.apply {
            expectThat(activeTab).isEqualTo(secondTab.tab)
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(secondTab.tab.currentDisplayUrl)
            expectThat(titleFlow.value).isEqualTo(secondTab.tab.currentDisplayTitle)
            expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
            expectThat(navigationInfoFlow.value.canGoForward).isTrue()
            expectThat(displayedInfoFlow.value.displayedText).isEqualTo("news.othersite.com")
            expectThat(displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        }
        expectThat(secondTab.tabCallbacks.size).isEqualTo(1)
        expectThat(secondTab.mockNavigationController.callbacks.size).isEqualTo(1)

        // The first tab's callbacks should have been unregistered.
        expectThat(mainTab.tabCallbacks).isEmpty()
        expectThat(mainTab.mockNavigationController.callbacks).isEmpty()
    }

    @Test
    fun onActiveTabChanged_updatesValuesAfterNullActiveTab() {
        model.apply {
            expectThat(activeTab).isNull()
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(Uri.EMPTY)
            expectThat(titleFlow.value).isEqualTo("")
            expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
        }

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        model.apply {
            expectThat(activeTab).isEqualTo(mainTab.tab)
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(mainTab.tab.currentDisplayUrl)
            expectThat(titleFlow.value).isEqualTo(mainTab.tab.currentDisplayTitle)
            expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            expectThat(displayedInfoFlow.value.displayedText).isEqualTo("www.site.com")
            expectThat(displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        }
        expectThat(mainTab.tabCallbacks.size).isEqualTo(1)
        expectThat(mainTab.mockNavigationController.callbacks.size).isEqualTo(1)

        // Say that there's no active tab anymore.
        model.onActiveTabChanged(null)
        model.apply {
            expectThat(activeTab).isEqualTo(null)
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(Uri.EMPTY)
            expectThat(titleFlow.value).isEqualTo("")
            expectThat(displayedInfoFlow.value.displayedText).isEqualTo("")
            expectThat(displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
            expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
        }

        // The first tab's callbacks should have been unregistered.
        expectThat(mainTab.tabCallbacks.size).isEqualTo(0)
        expectThat(mainTab.mockNavigationController.callbacks).isEmpty()
    }

    @Test
    fun mode_onNeevaHomepage_showsPlaceholder() {
        model.onActiveTabChanged(neevaHomepageTab.tab)
        expectThat(model.activeTab).isEqualTo(neevaHomepageTab.tab)

        model.apply {
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(neevaHomepageTab.tab.currentDisplayUrl)
            expectThat(titleFlow.value).isEqualTo(neevaHomepageTab.tab.currentDisplayTitle)
            expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            expectThat(displayedInfoFlow.value.displayedText).isEmpty()
            expectThat(displayedInfoFlow.value.mode).isEqualTo(DisplayMode.PLACEHOLDER)
        }
        expectThat(neevaHomepageTab.tabCallbacks).hasSize(1)
        expectThat(neevaHomepageTab.mockNavigationController.callbacks).hasSize(1)
    }

    @Test
    fun displayedText_showsDomainAndQuery() {
        // A tab with a Neeva search URL should show the search query in the URL bar.
        model.onActiveTabChanged(neevaSearchTab.tab)
        model.apply {
            expectThat(activeTab).isEqualTo(neevaSearchTab.tab)
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(neevaSearchTab.tab.currentDisplayUrl)
            expectThat(titleFlow.value).isEqualTo(neevaSearchTab.tab.currentDisplayTitle)
            expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            expectThat(displayedInfoFlow.value.displayedText).isEqualTo("query")
            expectThat(displayedInfoFlow.value.mode).isEqualTo(DisplayMode.QUERY)
        }
        expectThat(neevaSearchTab.tabCallbacks).hasSize(1)
        expectThat(neevaSearchTab.mockNavigationController.callbacks).hasSize(1)

        // A tab with a non-search URL should show the URL directly in the URL bar.
        model.onActiveTabChanged(neevaSpacesTab.tab)
        model.apply {
            expectThat(activeTab).isEqualTo(neevaSpacesTab.tab)
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(neevaSpacesTab.tab.currentDisplayUrl)
            expectThat(titleFlow.value).isEqualTo(neevaSpacesTab.tab.currentDisplayTitle)
            expectThat(navigationInfoFlow.value.canGoBackward).isTrue()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
            expectThat(displayedInfoFlow.value.displayedText).isEqualTo("neeva.com")
            expectThat(displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        }

        // The active tab callback should have swapped.
        expectThat(neevaSearchTab.tabCallbacks).isEmpty()
        expectThat(neevaSearchTab.mockNavigationController.callbacks).isEmpty()
        expectThat(neevaSpacesTab.tabCallbacks).hasSize(1)
        expectThat(neevaSpacesTab.mockNavigationController.callbacks).hasSize(1)
    }

    @Test
    fun onActiveTabChanged_monitorsChangesToTab() {
        model.apply {
            expectThat(activeTab).isNull()
            expectThat(progressFlow.value).isEqualTo(100)
            expectThat(urlFlow.value).isEqualTo(Uri.EMPTY)
            expectThat(titleFlow.value).isEqualTo("")
            expectThat(navigationInfoFlow.value.canGoBackward).isFalse()
            expectThat(navigationInfoFlow.value.canGoForward).isFalse()
        }

        // Set the first tab as active and confirm the flows were updated.
        model.onActiveTabChanged(mainTab.tab)
        model.apply {
            expectThat(model.activeTab).isEqualTo(mainTab.tab)
            expectThat(model.progressFlow.value).isEqualTo(100)
            expectThat(model.urlFlow.value).isEqualTo(mainTab.tab.currentDisplayUrl)
            expectThat(model.titleFlow.value).isEqualTo(mainTab.tab.currentDisplayTitle)
            expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()
            expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()
            expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("www.site.com")
            expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)
        }
        expectThat(mainTab.tabCallbacks).hasSize(1)
        expectThat(mainTab.mockNavigationController.callbacks).hasSize(1)

        // Check that the title gets updated.
        mainTab.tabCallbacks.first().onTitleUpdated("New title")
        expectThat(model.titleFlow.value).isEqualTo("New title")

        // Check that the Uri gets updated.
        mainTab.tabCallbacks.first().onVisibleUriChanged(Uri.parse("http://news.othersite.com/2"))
        expectThat(model.urlFlow.value).isEqualTo(Uri.parse("http://news.othersite.com/2"))
        expectThat(model.displayedInfoFlow.value.displayedText).isEqualTo("news.othersite.com")
        expectThat(model.displayedInfoFlow.value.mode).isEqualTo(DisplayMode.URL)

        // Check that back and forward statuses are kept in sync.
        mainTab.mockNavigationController.controller.goBack()
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()
        expectThat(model.navigationInfoFlow.value.canGoForward).isTrue()

        mainTab.mockNavigationController.controller.goForward()
        expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()
        expectThat(model.navigationInfoFlow.value.canGoForward).isFalse()

        // Check that the load progress is updated.
        mainTab.mockNavigationController.callbacks.forEach { it.onLoadProgressChanged(0.429) }
        expectThat(model.progressFlow.value).isEqualTo(43)
        verify(tabScreenshotManager, never()).captureAndSaveScreenshot(any(), any())

        mainTab.mockNavigationController.callbacks.forEach { it.onLoadProgressChanged(1.0) }
        expectThat(model.progressFlow.value).isEqualTo(100)
        verify(tabScreenshotManager).captureAndSaveScreenshot(any(), any())
    }

    @Test
    fun reload_withActiveTab_reloadsTab() {
        model.onActiveTabChanged(mainTab.tab)
        model.reload()
        verify(mainTab.mockNavigationController.controller, times(1)).reload()
    }

    @Test
    fun goBack() {
        // Set the tab.
        val harness = MockHarness(
            navigations = listOf(Uri.parse("https://www.site.com/"))
        )
        model.onActiveTabChanged(harness.tab)

        // There is no way to go back because there's only one URL.
        val firstGoBackResult = model.goBack()
        verify(harness.mockNavigationController.controller, never()).goBack()
        expectThat(firstGoBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = null,
                spaceIdToOpen = null,
                originalSearchQuery = null
            )
        )

        // Say that the user navigated somewhere and we can now go backward.
        harness.mockNavigationController.controller.navigate(Uri.parse("http://anywhere.else"))

        val secondGoBackResult = model.goBack()
        verify(harness.mockNavigationController.controller, times(1)).goBack()
        expectThat(secondGoBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = null,
                spaceIdToOpen = null,
                originalSearchQuery = null
            )
        )
    }

    @Test
    fun goBack_forChildTab() {
        // Set the tab.
        val parentTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.techmeme.com/")),
            tabId = "parent tab"
        )
        val childTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.sitelinkedfromtechmeme.com/")),
            tabId = "child tab",
            parentTabId = parentTabHarness.tabId
        )

        model.onActiveTabChanged(childTabHarness.tab)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()

        // Hit "back" on the child tab.
        val goBackResult = model.goBack()

        // Even though there are no navigations on this tab, because it is a child we should have
        // closed it.
        verify(childTabHarness.mockNavigationController.controller, never()).goBack()
        expectThat(goBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = childTabHarness.tabId,
                spaceIdToOpen = null,
                originalSearchQuery = null
            )
        )
    }

    @Test
    fun goBack_forChildTabWithSearchQuery_reshowsQuery() {
        // Create two tabs, with the second tab spawning from the first because of a search query.
        val parentTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.techmeme.com/")),
            tabId = "parent tab"
        )
        val childTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.sitelinkedfromtechmeme.com/")),
            tabId = "child tab",
            parentTabId = parentTabHarness.tabId
        )
        tabList.updateQueryNavigation(
            tabId = childTabHarness.tabId,
            navigationEntryIndex = 0,
            navigationEntryUri = Uri.parse("https://www.sitelinkedfromtechmeme.com/"),
            searchQuery = "triggering query"
        )

        model.onActiveTabChanged(childTabHarness.tab)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()

        // Hit "back" on the child tab.
        val goBackResult = model.goBack()

        // Even though there are no navigations on this tab, because it is a child we should have
        // closed it.
        verify(childTabHarness.mockNavigationController.controller, never()).goBack()
        expectThat(goBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = childTabHarness.tabId,
                spaceIdToOpen = null,
                originalSearchQuery = "triggering query"
            )
        )
    }

    @Test
    fun goBack_forChildTabOfSpace_reshowsSpace() {
        Mockito.`when`(spaceStore.doesSpaceExist("parentSpaceId")).thenReturn(true)

        // Create a tab that is spawned from a click on a Space.
        val childTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.sitelinkedfromtechmeme.com/")),
            tabId = "child tab",
            parentSpaceId = "parentSpaceId"
        )

        model.onActiveTabChanged(childTabHarness.tab)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()

        // Hit "back" on the child tab.
        val goBackResult = model.goBack()

        // Even though there are no navigations on this tab, because it is a child we should have
        // closed it.
        verify(childTabHarness.mockNavigationController.controller, never()).goBack()
        expectThat(goBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = childTabHarness.tabId,
                spaceIdToOpen = "parentSpaceId",
                originalSearchQuery = null
            )
        )
    }

    @Test
    fun goBack_forChildTabOfMissingSpace_doesNotReshowSpace() {
        Mockito.`when`(spaceStore.doesSpaceExist("parentSpaceId")).thenReturn(false)

        // Create a tab that is spawned from a click on a Space.
        val childTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.sitelinkedfromtechmeme.com/")),
            tabId = "child tab",
            parentSpaceId = "parentSpaceId"
        )

        model.onActiveTabChanged(childTabHarness.tab)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isFalse()

        // Hit "back" on the child tab.  Nothing should have happened.
        val goBackResult = model.goBack()
        verify(childTabHarness.mockNavigationController.controller, never()).goBack()
        expectThat(goBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = null,
                spaceIdToOpen = null,
                originalSearchQuery = null
            )
        )
    }

    @Test
    fun goBack_forChildTabWithoutParent_stillReshowsQuery() {
        // Create two tabs, with the second tab spawning from the first because of a search query.
        val parentTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.techmeme.com/")),
            tabId = "parent tab"
        )
        val childTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.sitelinkedfromtechmeme.com/")),
            tabId = "child tab",
            parentTabId = parentTabHarness.tabId
        )
        tabList.updateQueryNavigation(
            tabId = childTabHarness.tabId,
            navigationEntryIndex = 0,
            navigationEntryUri = Uri.parse("https://www.sitelinkedfromtechmeme.com/"),
            searchQuery = "triggering query"
        )

        // Remove the parent tab.
        tabList.remove(parentTabHarness.tabId)

        model.onActiveTabChanged(childTabHarness.tab)
        expectThat(model.navigationInfoFlow.value.canGoBackward).isTrue()

        // Hit "back" on the child tab.
        val goBackResult = model.goBack()

        // This tab was created to show search results.  Make sure it's closed and reshow the query.
        verify(childTabHarness.mockNavigationController.controller, never()).goBack()
        expectThat(goBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = childTabHarness.tabId,
                spaceIdToOpen = null,
                originalSearchQuery = "triggering query"
            )
        )
    }

    @Test
    fun goBack_forChildTabAfterParentClosed_doesNothing() {
        // Set the tab.
        val parentTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.techmeme.com/")),
            tabId = "parent tab"
        )
        val childTabHarness = MockHarness(
            navigations = listOf(Uri.parse("https://www.sitelinkedfromtechmeme.com/")),
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
        val goBackResult = model.goBack()

        // Nothing should happen.
        verify(childTabHarness.mockNavigationController.controller, never()).goBack()
        expectThat(goBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = null,
                spaceIdToOpen = null,
                originalSearchQuery = null
            )
        )
    }

    @Test
    fun goBack_whenQueryExists_reshowsSuggestions() {
        val harness = MockHarness(
            navigations = listOf(
                Uri.parse("https://www.first.com"),
                Uri.parse("http://www.second.com"),
                Uri.parse("http://third.net"),
            )
        )
        model.onActiveTabChanged(harness.tab)

        // Say that the user got to the last two navigations via SAYT.
        tabList.updateQueryNavigation(
            tabId = harness.tabId,
            navigationEntryIndex = 1,
            navigationEntryUri = Uri.parse("http://www.second.com"),
            searchQuery = "first search term"
        )
        tabList.updateQueryNavigation(
            tabId = harness.tabId,
            navigationEntryIndex = 2,
            navigationEntryUri = Uri.parse("http://third.net"),
            searchQuery = "second search term"
        )

        // Go back.  We should be asked to show search results.
        val firstGoBackResult = model.goBack()
        verify(harness.mockNavigationController.controller).goBack()
        expectThat(firstGoBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = null,
                spaceIdToOpen = null,
                originalSearchQuery = "second search term"
            )
        )

        val secondGoBackResult = model.goBack()
        verify(harness.mockNavigationController.controller, times(2)).goBack()
        expectThat(secondGoBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = null,
                spaceIdToOpen = null,
                originalSearchQuery = "first search term"
            )
        )

        // Navigate somewhere to clear out the navigation history.
        harness.mockNavigationController.controller.navigate(Uri.parse("https://www.fourth.com"))

        expectThat(harness.mockNavigationController.controller.getNavigationEntryDisplayUri(0))
            .isEqualTo(Uri.parse("https://www.first.com"))
        expectThat(harness.mockNavigationController.controller.getNavigationEntryDisplayUri(1))
            .isEqualTo(Uri.parse("https://www.fourth.com"))
        expectThat(harness.mockNavigationController.controller.navigationListCurrentIndex)
            .isEqualTo(1)

        // We shouldn't have tried to reshow the search results because the queries are gone.
        val thirdGoBackResult = model.goBack()
        verify(harness.mockNavigationController.controller, times(3)).goBack()
        expectThat(thirdGoBackResult).isEqualTo(
            GoBackResult(
                tabIdToClose = null,
                spaceIdToOpen = null,
                originalSearchQuery = null
            )
        )
    }

    @Test
    fun goForward() {
        // Set the tab.
        val harness = MockHarness(
            navigations = listOf(Uri.parse("https://www.site.com/"))
        )
        model.onActiveTabChanged(harness.tab)

        model.goForward()
        verify(harness.mockNavigationController.controller, never()).goForward()

        // Say that the user navigated somewhere and we can now go forward.
        harness.mockNavigationController.controller.navigate(Uri.parse("http://anywhere.else"))
        harness.mockNavigationController.controller.goBack()

        model.goForward()
        verify(harness.mockNavigationController.controller, times(1)).goForward()
    }

    private var nextMockTabId: Int = 0
    inner class MockHarness(
        val tabId: String = (nextMockTabId++).toString(),
        val parentTabId: String? = null,
        val parentSpaceId: String? = null,
        val navigations: List<Uri> = emptyList()
    ) {
        val tabCallbacks = mutableSetOf<TabCallback>()

        val mockNavigationController = MockNavigationController().apply {
            navigations.forEach { recordNavigation(it) }
        }

        val tab = mock<Tab> {
            on { guid } doReturn tabId

            on { navigationController } doReturn mockNavigationController.controller

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
                parentSpaceId = parentSpaceId,
                tabOpenType = parentTabId?.let { TabOpenType.CHILD_TAB } ?: TabOpenType.DEFAULT
            )
        }
    }
}
