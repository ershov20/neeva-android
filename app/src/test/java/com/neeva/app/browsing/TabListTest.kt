package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.BaseTest
import com.neeva.app.createMockNavigationController
import org.chromium.weblayer.Browser
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
class TabListTest : BaseTest() {
    private lateinit var tabList: TabList

    private lateinit var browser: Browser
    private lateinit var firstTab: Tab
    private lateinit var secondTab: Tab

    override fun setUp() {
        super.setUp()
        tabList = TabList()

        browser = mock<Browser> {
            on { isDestroyed } doReturn false
        }

        val firstNavigationController = mock<NavigationController> {
            on { navigationListSize } doReturn 1
            on { navigationListCurrentIndex } doReturn 0
            on { getNavigationEntryDisplayUri(eq(0)) } doReturn Uri.parse("http://example.com")
            on { getNavigationEntryTitle(eq(0)) } doReturn "Example"
        }

        val secondNavigationController = mock<NavigationController> {
            on { navigationListSize } doReturn 2
            on { navigationListCurrentIndex } doReturn 1
            on { getNavigationEntryDisplayUri(eq(1)) } doReturn Uri.parse("http://thing.com")
            on { getNavigationEntryTitle(eq(1)) } doReturn "Thing"
        }

        firstTab = mock {
            on { getNavigationController() } doReturn firstNavigationController
            on { getBrowser() } doReturn browser
            on { guid } doReturn "tab guid 1"
            on { isDestroyed } doReturn false
            on { data } doReturn emptyMap()
        }

        secondTab = mock {
            on { getNavigationController() } doReturn secondNavigationController
            on { getBrowser() } doReturn browser
            on { guid } doReturn "tab guid 2"
            on { isDestroyed } doReturn false
            on { data } doReturn mapOf(
                TabInfo.PersistedData.KEY_PARENT_TAB_ID to "tab guid 1",
                TabInfo.PersistedData.KEY_OPEN_TYPE to TabInfo.TabOpenType.CHILD_TAB.name
            )
        }
    }

    @Test
    fun addAndRemove() {
        `when`(browser.activeTab).doReturn(firstTab)

        expectThat(tabList.orderedTabList.value).isEmpty()
        tabList.add(firstTab)
        tabList.add(secondTab)

        expectThat(tabList.orderedTabList.value).containsExactly(
            TabInfo(
                id = "tab guid 1",
                url = Uri.parse("http://example.com"),
                title = "Example",
                isSelected = true,
                isCrashed = false,
                data = TabInfo.PersistedData(
                    parentTabId = null,
                    openType = TabInfo.TabOpenType.DEFAULT
                )
            ),
            TabInfo(
                id = "tab guid 2",
                url = Uri.parse("http://thing.com"),
                title = "Thing",
                isSelected = false,
                isCrashed = false,
                data = TabInfo.PersistedData(
                    parentTabId = "tab guid 1",
                    openType = TabInfo.TabOpenType.CHILD_TAB
                )
            )
        )

        tabList.remove(firstTab.guid)
        expectThat(tabList.orderedTabList.value).containsExactlyInAnyOrder(
            TabInfo(
                id = "tab guid 2",
                url = Uri.parse("http://thing.com"),
                title = "Thing",
                isSelected = false,
                isCrashed = false,
                data = TabInfo.PersistedData(
                    parentTabId = "tab guid 1",
                    openType = TabInfo.TabOpenType.CHILD_TAB
                )
            )
        )
    }

    @Test
    fun updateSelectedTab() {
        `when`(browser.activeTab).doReturn(firstTab)

        expectThat(tabList.orderedTabList.value).isEmpty()
        tabList.add(firstTab)
        tabList.add(secondTab)

        expectThat(tabList.getTabInfo(firstTab.guid)?.isSelected).isTrue()
        expectThat(tabList.getTabInfo(secondTab.guid)?.isSelected).isFalse()

        tabList.updatedSelectedTab(secondTab.guid)
        expectThat(tabList.getTabInfo(firstTab.guid)?.isSelected).isFalse()
        expectThat(tabList.getTabInfo(secondTab.guid)?.isSelected).isTrue()
    }

    @Test
    fun updateTabTitle() {
        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)

        expectThat(tabList.getTabInfo(firstTab.guid)?.title).isEqualTo("Example")
        expectThat(tabList.getTabInfo(secondTab.guid)?.title).isEqualTo("Thing")

        tabList.updateTabTitle(firstTab.guid, "New title")
        expectThat(tabList.getTabInfo(firstTab.guid)?.title).isEqualTo("New title")
        expectThat(tabList.getTabInfo(secondTab.guid)?.title).isEqualTo("Thing")
    }

    @Test
    fun updateUrl() {
        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)

        expectThat(tabList.getTabInfo(firstTab.guid)?.url)
            .isEqualTo(Uri.parse("http://example.com"))
        expectThat(tabList.getTabInfo(secondTab.guid)?.url)
            .isEqualTo(Uri.parse("http://thing.com"))

        tabList.updateUrl(secondTab.guid, Uri.parse("http://updated.com"))
        expectThat(tabList.getTabInfo(firstTab.guid)?.url)
            .isEqualTo(Uri.parse("http://example.com"))
        expectThat(tabList.getTabInfo(secondTab.guid)?.url)
            .isEqualTo(Uri.parse("http://updated.com"))
    }

    @Test
    fun updateIsCrashed() {
        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)

        expectThat(tabList.getTabInfo(firstTab.guid)?.isCrashed).isFalse()
        expectThat(tabList.getTabInfo(secondTab.guid)?.isCrashed).isFalse()

        tabList.updateIsCrashed(secondTab.guid, true)
        expectThat(tabList.getTabInfo(firstTab.guid)?.isCrashed).isFalse()
        expectThat(tabList.getTabInfo(secondTab.guid)?.isCrashed).isTrue()
    }

    @Test
    fun updateIsClosing() {
        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)

        expectThat(tabList.getTabInfo(firstTab.guid)?.isClosing).isFalse()
        expectThat(tabList.getTabInfo(secondTab.guid)?.isClosing).isFalse()

        tabList.updateIsClosing(firstTab.guid, true)
        expectThat(tabList.getTabInfo(firstTab.guid)?.isClosing).isTrue()
        expectThat(tabList.getTabInfo(secondTab.guid)?.isClosing).isFalse()
    }

    @Test
    fun updateParentInfo() {
        val thirdNavigationController = mock<NavigationController> {
            on { navigationListSize } doReturn 3
            on { navigationListCurrentIndex } doReturn 1
            on { getNavigationEntryDisplayUri(eq(1)) } doReturn Uri.parse("http://else.com")
            on { getNavigationEntryTitle(eq(1)) } doReturn "Else"
        }

        val thirdTab = mock<Tab> {
            on { getNavigationController() } doReturn thirdNavigationController
            on { getBrowser() } doReturn browser
            on { guid } doReturn "tab guid 3"
            on { isDestroyed } doReturn false
            on { data } doReturn emptyMap()
        }

        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)
        tabList.add(thirdTab)

        expectThat(tabList.isParentTabInList(thirdTab.guid)).isFalse()

        tabList.updateParentInfo(thirdTab, firstTab.guid, TabInfo.TabOpenType.CHILD_TAB)
        expectThat(tabList.isParentTabInList(thirdTab.guid)).isTrue()
    }

    @Test
    fun isParentInTabList() {
        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)

        expectThat(tabList.isParentTabInList(firstTab.guid)).isFalse()
        expectThat(tabList.isParentTabInList(secondTab.guid)).isTrue()

        tabList.remove(firstTab.guid)
        expectThat(tabList.isParentTabInList(secondTab.guid)).isFalse()
    }

    @Test
    fun updateQueryNavigation() {
        tabList.add(firstTab)
        tabList.add(secondTab)

        tabList.updateQueryNavigation(
            tabId = firstTab.guid,
            navigationEntryIndex = 5,
            navigationEntryUri = Uri.parse("http://www.query1.com"),
            searchQuery = "query 1"
        )

        tabList.updateQueryNavigation(
            tabId = firstTab.guid,
            navigationEntryIndex = 8,
            navigationEntryUri = Uri.parse("http://www.query2.com"),
            searchQuery = "query 2"
        )

        // Confirm that multiple queries are recorded.
        tabList.getTabInfo(firstTab.guid)!!.searchQueryMap.apply {
            expectThat(this).hasSize(2)
            expectThat(this[1]).isNull()

            expectThat(this[5]).isEqualTo(
                SearchNavigationInfo(
                    navigationEntryIndex = 5,
                    navigationEntryUri = Uri.parse("http://www.query1.com"),
                    searchQuery = "query 1"
                )
            )

            expectThat(this[8]).isEqualTo(
                SearchNavigationInfo(
                    navigationEntryIndex = 8,
                    navigationEntryUri = Uri.parse("http://www.query2.com"),
                    searchQuery = "query 2"
                )
            )
        }

        // Say that the navigation history is now missing the URI that triggered query 2.
        val navigationController = createMockNavigationController()
        navigationController.navigate(Uri.parse("https://0.com"))
        navigationController.navigate(Uri.parse("https://1.com"))
        navigationController.navigate(Uri.parse("https://2.com"))
        navigationController.navigate(Uri.parse("https://3.com"))
        navigationController.navigate(Uri.parse("https://4.com"))
        navigationController.navigate(Uri.parse("http://www.query1.com"))
        tabList.pruneQueries(firstTab.guid, navigationController)

        // The second query should be wiped out.
        tabList.getTabInfo(firstTab.guid)!!.searchQueryMap.apply {
            expectThat(this).hasSize(1)
            expectThat(this[1]).isNull()

            expectThat(this[5]).isEqualTo(
                SearchNavigationInfo(
                    navigationEntryIndex = 5,
                    navigationEntryUri = Uri.parse("http://www.query1.com"),
                    searchQuery = "query 1"
                )
            )

            expectThat(this[8]).isNull()
        }

        // Say that the navigation history now has a different URI than was recorded for the query.
        val secondController = createMockNavigationController()
        secondController.navigate(Uri.parse("https://0.com"))
        secondController.navigate(Uri.parse("https://1.com"))
        secondController.navigate(Uri.parse("https://2.com"))
        secondController.navigate(Uri.parse("https://3.com"))
        secondController.navigate(Uri.parse("https://4.com"))
        secondController.navigate(Uri.parse("https://5.com"))
        tabList.pruneQueries(firstTab.guid, secondController)

        // The queries should all be gone.
        tabList.getTabInfo(firstTab.guid)!!.searchQueryMap.apply {
            expectThat(this).isEmpty()
        }
    }

    @Test
    fun clear() {
        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)
        expectThat(tabList.orderedTabList.value).hasSize(2)

        tabList.clear()
        expectThat(tabList.orderedTabList.value).isEmpty()
        expectThat(tabList.getTabInfo(firstTab.guid)).isNull()
        expectThat(tabList.getTabInfo(secondTab.guid)).isNull()
    }
}
