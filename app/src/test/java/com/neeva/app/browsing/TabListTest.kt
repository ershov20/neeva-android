package com.neeva.app.browsing

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.storage.entities.SearchNavigation
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Browser
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class TabListTest : BaseTest() {
    private lateinit var tabList: TabList

    private lateinit var browser: Browser
    private lateinit var firstTab: Tab
    private lateinit var secondTab: Tab
    private lateinit var thirdTab: Tab

    private lateinit var removeQueryNavigationsCalls: MutableList<String>

    override fun setUp() {
        super.setUp()

        removeQueryNavigationsCalls = mutableListOf()

        tabList = object : TabList() {
            override val searchNavigationMap: StateFlow<Map<String, List<SearchNavigation>>>
                get() = TODO("Not yet implemented")

            override fun updateQueryNavigation(
                tabId: String,
                navigationEntryIndex: Int,
                navigationEntryUri: Uri,
                searchQuery: String?
            ) = TODO("Not yet implemented")

            override fun removeQueryNavigations(tabId: String) {
                removeQueryNavigationsCalls.add(tabId)
            }

            override fun pruneQueryNavigations() = TODO("Not yet implemented")

            override fun pruneQueryNavigations(
                tabId: String,
                navigationController: NavigationController
            ) = TODO("Not yet implemented")
        }

        browser = mock {
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

        val thirdNavigationController = mock<NavigationController> {
            on { navigationListSize } doReturn 1
            on { navigationListCurrentIndex } doReturn 0
            on { getNavigationEntryDisplayUri(eq(0)) } doReturn Uri.parse("http://example.com")
            on { getNavigationEntryTitle(eq(0)) } doReturn "Example"
        }

        firstTab = mock {
            on { getNavigationController() } doReturn firstNavigationController
            on { getBrowser() } doReturn browser
            on { guid } doReturn "tab guid 1"
            on { isDestroyed } doReturn false
            on { data } doReturn mapOf(
                TabInfo.PersistedData.KEY_LAST_ACTIVE_MS to "1000000"
            )
        }

        secondTab = mock {
            on { getNavigationController() } doReturn secondNavigationController
            on { getBrowser() } doReturn browser
            on { guid } doReturn "tab guid 2"
            on { isDestroyed } doReturn false
            on { data } doReturn mapOf(
                TabInfo.PersistedData.KEY_PARENT_TAB_ID to "tab guid 1",
                TabInfo.PersistedData.KEY_OPEN_TYPE to TabInfo.TabOpenType.CHILD_TAB.name,
                TabInfo.PersistedData.KEY_LAST_ACTIVE_MS to "2000000"
            )
        }

        thirdTab = mock {
            on { getNavigationController() } doReturn thirdNavigationController
            on { getBrowser() } doReturn browser
            on { guid } doReturn "tab guid 3"
            on { isDestroyed } doReturn false
            on { data } doReturn mapOf(
                TabInfo.PersistedData.KEY_LAST_ACTIVE_MS to "3000000"
            )
        }
    }

    @Test
    fun addAndRemove() {
        `when`(browser.activeTab).doReturn(firstTab)

        expectThat(tabList.orderedTabList.value).isEmpty()
        tabList.add(firstTab)
        tabList.add(secondTab)

        val actualTabs = tabList.orderedTabList.value
        expectThat(actualTabs).hasSize(2)

        // Because the tab is selected, it should update its "last active" timestamp.
        val updatedTab = actualTabs[0]
        expectThat(updatedTab.id).isEqualTo(firstTab.guid)
        expectThat(updatedTab.data.lastActiveMs!! > 1_000_000L).isTrue()

        val updatedTimestamp = updatedTab.data.lastActiveMs!!
        expectThat(actualTabs[0]).isEqualTo(
            TabInfo(
                id = "tab guid 1",
                url = Uri.parse("http://example.com"),
                title = "Example",
                isSelected = true,
                isCrashed = false,
                data = TabInfo.PersistedData(
                    parentTabId = null,
                    openType = TabInfo.TabOpenType.DEFAULT,
                    lastActiveMs = updatedTimestamp
                )
            )
        )

        expectThat(actualTabs[1]).isEqualTo(
            TabInfo(
                id = "tab guid 2",
                url = Uri.parse("http://thing.com"),
                title = "Thing",
                isSelected = false,
                isCrashed = false,
                data = TabInfo.PersistedData(
                    parentTabId = "tab guid 1",
                    openType = TabInfo.TabOpenType.CHILD_TAB,
                    lastActiveMs = 2_000_000L
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
                    openType = TabInfo.TabOpenType.CHILD_TAB,
                    lastActiveMs = 2_000_000L
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

        tabList.updateParentInfo(
            tab = thirdTab,
            parentTabId = firstTab.guid,
            parentSpaceId = null,
            tabOpenType = TabInfo.TabOpenType.CHILD_TAB
        )
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
    fun clear() {
        `when`(browser.activeTab).doReturn(firstTab)
        tabList.add(firstTab)
        tabList.add(secondTab)
        expectThat(tabList.orderedTabList.value).hasSize(2)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://example.com"))).isNotNull()
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://thing.com"))).isNotNull()

        tabList.clear()
        expectThat(tabList.orderedTabList.value).isEmpty()
        expectThat(tabList.getTabInfo(firstTab.guid)).isNull()
        expectThat(tabList.getTabInfo(secondTab.guid)).isNull()
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://example.com"))).isNull()
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://thing.com"))).isNull()
    }

    @Test
    fun fuzzyMapStaysUpdated() {
        `when`(browser.activeTab).doReturn(firstTab)

        // Add two tabs with the same URL and one with a different URL.
        tabList.add(firstTab)
        tabList.add(secondTab)
        tabList.add(thirdTab)

        // Confirm that the map finds the right tabs for compatible URIs.
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://example.com")))
            .isEqualTo(firstTab.guid)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("https://www.example.com")))
            .isEqualTo(firstTab.guid)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("file:www.example.com")))
            .isNull()

        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://thing.com")))
            .isEqualTo(secondTab.guid)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("https://www.thing.com")))
            .isEqualTo(secondTab.guid)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("file:www.thing.com")))
            .isNull()

        // Update the first tab's URI, which should update the map for the first tab.
        tabList.updateUrl(firstTab.guid, Uri.parse("https://www.reddit.com"))
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://example.com")))
            .isEqualTo(thirdTab.guid)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://reddit.com")))
            .isEqualTo(firstTab.guid)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://thing.com")))
            .isEqualTo(secondTab.guid)

        // Removing a tab from the list should result in the fuzzy match failing.
        tabList.remove(secondTab.guid)
        expectThat(tabList.findTabWithSimilarUri(Uri.parse("http://thing.com")))
            .isNull()
    }
}
