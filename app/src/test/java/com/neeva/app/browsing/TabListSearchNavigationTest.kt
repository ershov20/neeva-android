package com.neeva.app.browsing

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.MockNavigationController
import com.neeva.app.createMockNavigationController
import com.neeva.app.storage.daos.SearchNavigationDao
import com.neeva.app.storage.entities.SearchNavigation
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.chromium.weblayer.Browser
import org.chromium.weblayer.Tab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

typealias SearchNavigationMap = MutableMap<String, MutableList<SearchNavigation>>

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RegularTabListTest : BaseTest() {
    @get:Rule
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var tabList: TabList

    private lateinit var mockBrowser: Browser
    private lateinit var firstTab: Tab
    private lateinit var secondTab: Tab
    private lateinit var searchNavigationDao: SearchNavigationDao

    private lateinit var entries: MutableList<SearchNavigation>
    private lateinit var entriesFlow: MutableStateFlow<SearchNavigationMap>

    override fun setUp() {
        super.setUp()

        entries = mutableListOf()
        entriesFlow = MutableStateFlow(mutableMapOf())

        searchNavigationDao = mockk {
            every { getAllMapFlow(any(), any()) } answers { entriesFlow }

            every { add(any()) } answers {
                entries.add(it.invocation.args[0] as SearchNavigation)
                entriesFlow.value = SearchNavigationDao.listToMap(entries)
            }

            every { delete(any(), any()) } answers {
                val tabId = it.invocation.args[0] as String
                val index = it.invocation.args[1] as Int
                entries.removeIf { entry ->
                    entry.tabId == tabId && entry.navigationEntryIndex == index
                }
                entriesFlow.value = SearchNavigationDao.listToMap(entries)
            }

            every { deleteAllForTab(any()) } answers {
                val tabId = it.invocation.args[0] as String
                entries.removeIf { entry -> entry.tabId == tabId }
                entriesFlow.value = SearchNavigationDao.listToMap(entries)
            }

            every { deleteAllForTabs(any()) } answers {
                val tabIds = it.invocation.args[0] as List<*>
                entries.removeIf { entry -> entry.tabId in tabIds }
                entriesFlow.value = SearchNavigationDao.listToMap(entries)
            }

            every { prune(any(), any()) } answers {
                val tabId = it.invocation.args[0] as String
                val index = it.invocation.args[1] as Int
                entries.removeIf { entry ->
                    entry.tabId == tabId && entry.navigationEntryIndex >= index
                }
                entriesFlow.value = SearchNavigationDao.listToMap(entries)
            }
        }

        mockBrowser = mockk {
            every { isDestroyed } returns false
            every { activeTab } returns null
        }

        firstTab = mockk {
            every { browser } returns mockBrowser
            every { navigationController } returns createMockNavigationController()
            every { guid } returns "tab guid 1"
            every { data } returns emptyMap()
        }

        secondTab = mockk {
            every { browser } returns mockBrowser
            every { navigationController } returns createMockNavigationController()
            every { guid } returns "tab guid 2"
            every { data } returns emptyMap()
        }
    }

    @Test
    fun updateQueryNavigation() {
        tabList = RegularTabList(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            searchNavigationDao = searchNavigationDao
        )
        runTest()
    }

    @Test
    fun updateQueryNavigationIncognito() {
        tabList = IncognitoTabList()
        runTest()
    }

    private fun runTest() {
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
        coroutineScopeRule.advanceUntilIdle()

        // Confirm that multiple queries are recorded.
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 1)).isNull()
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 5)).isEqualTo(
            SearchNavigation(
                tabId = firstTab.guid,
                navigationEntryIndex = 5,
                navigationEntryUri = Uri.parse("http://www.query1.com"),
                searchQuery = "query 1"
            )
        )
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 8)).isEqualTo(
            SearchNavigation(
                tabId = firstTab.guid,
                navigationEntryIndex = 8,
                navigationEntryUri = Uri.parse("http://www.query2.com"),
                searchQuery = "query 2"
            )
        )

        // Say that the navigation history is now missing the URI that triggered query 2.
        val mockNavigationController = MockNavigationController()
        val navigationController = mockNavigationController.controller
        navigationController.navigate(Uri.parse("https://0.com"))
        navigationController.navigate(Uri.parse("https://1.com"))
        navigationController.navigate(Uri.parse("https://2.com"))
        navigationController.navigate(Uri.parse("https://3.com"))
        navigationController.navigate(Uri.parse("https://4.com"))
        navigationController.navigate(Uri.parse("http://www.query1.com"))

        tabList.pruneQueryNavigations(firstTab.guid, navigationController)
        coroutineScopeRule.advanceUntilIdle()

        // The second query should be wiped out.
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 1)).isNull()
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 5)).isEqualTo(
            SearchNavigation(
                tabId = firstTab.guid,
                navigationEntryIndex = 5,
                navigationEntryUri = Uri.parse("http://www.query1.com"),
                searchQuery = "query 1"
            )
        )
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 8)).isNull()

        // Prune the list further.
        val secondController = createMockNavigationController()
        secondController.navigate(Uri.parse("https://0.com"))
        secondController.navigate(Uri.parse("https://1.com"))
        secondController.navigate(Uri.parse("https://2.com"))
        secondController.navigate(Uri.parse("https://3.com"))
        secondController.navigate(Uri.parse("https://4.com"))

        tabList.pruneQueryNavigations(firstTab.guid, secondController)
        coroutineScopeRule.advanceUntilIdle()

        // The queries should all be gone.
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 1)).isNull()
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 5)).isNull()
        expectThat(tabList.getSearchNavigationInfo(firstTab.guid, 8)).isNull()
    }
}
