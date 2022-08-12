package com.neeva.app.browsing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.chromium.weblayer.Browser
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BrowserRestoreCallbackImplTest {
    @Test
    fun onRestoreCompleted_withoutTabs_firesEmptyTabList() {
        // Arrange: Say that the browser had no tabs.
        val testSetup = TestSetup(
            inactiveTabIds = emptyList(),
            inactiveTabTimestamps = emptyList(),
            activeTabId = null
        )

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: We should have received a callback about there being no tabs to restore.
        verify(testSetup.restoreCompletedCallback).invoke()
        verify(testSetup.tabList).pruneQueryNavigations()
        verify(testSetup.onEmptyTabList, times(1)).invoke()

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_withSingleActiveTabAndInvalidNavigation_goesHome() {
        // Arrange: Say that the browser had only one tab, it was active, and in a bad state.
        val activeTabId = "tab b"
        val testSetup = TestSetup(
            inactiveTabIds = emptyList(),
            inactiveTabTimestamps = emptyList(),
            activeTabId = activeTabId,
            activeTabNavigationIndex = -1
        )

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: It should have done something with the tab.
        verify(testSetup.restoreCompletedCallback).invoke()
        verify(testSetup.tabList).pruneQueryNavigations()
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.onBlankTabCreated).invoke(any())

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_withSingleActiveTabAndValidNavigation_staysPut() {
        // Arrange: Say that the browser had only one tab, it was active, and in a good state.
        val activeTabId = "tab b"
        val testSetup = TestSetup(
            inactiveTabIds = emptyList(),
            inactiveTabTimestamps = emptyList(),
            activeTabId = activeTabId,
            activeTabNavigationIndex = 13
        )

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: It should not have done anything with the tab.
        verify(testSetup.restoreCompletedCallback).invoke()
        verify(testSetup.tabList).pruneQueryNavigations()
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.onBlankTabCreated, never()).invoke(any())

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_withActiveTabAndInvalidNavigation_doesNothing() {
        // Arrange: Say that the browser had three tabs and that the active tab was in a bad state.
        val testSetup = TestSetup(
            inactiveTabIds = listOf("tab a", "tab c"),
            inactiveTabTimestamps = listOf(1_000_000L, 2_000_000L),
            activeTabId = "tab b"
        )

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: Because there were multiple tabs restored, "blank tab" logic shouldn't kick in.
        verify(testSetup.restoreCompletedCallback).invoke()
        verify(testSetup.tabList).pruneQueryNavigations()
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.onBlankTabCreated, never()).invoke(any())

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_restoresTabData() {
        val inactiveTabData = listOf(
            Pair("tab a", 1_000_000L),
            Pair("tab c", null),
            Pair("tab d", 2_000_000L),
        )

        // Arrange: Say that the browser had four tabs.
        val testSetup = TestSetup(
            inactiveTabIds = inactiveTabData.map { it.first },
            inactiveTabTimestamps = inactiveTabData.map { it.second },
            activeTabId = "tab b"
        )

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: Confirm that the data was pulled back out correctly.
        verify(testSetup.restoreCompletedCallback).invoke()
        verify(testSetup.tabList).pruneQueryNavigations()
        testSetup.tabs.forEach { tab ->
            val actualData = argumentCaptor<TabInfo.PersistedData>()
            val expectedData = TabInfo.PersistedData(
                isSelected = tab.isSelected,
                map = tab.data
            )

            when (tab.guid) {
                "tab a", "tab d" -> {
                    // Inactive tabs with persisted timestamps should restore them.
                    // Confirm that the data was restored correctly.
                    verify(testSetup.tabList).setPersistedInfo(tab, expectedData, false)
                }

                "tab b" -> {
                    // Active tabs should get an updated timestamp.
                    verify(testSetup.tabList).setPersistedInfo(
                        eq(tab),
                        actualData.capture(),
                        eq(true)
                    )
                    expectThat(actualData.lastValue).isEqualTo(
                        expectedData.copy(lastActiveMs = actualData.lastValue.lastActiveMs)
                    )
                }

                "tab c" -> {
                    // The timestamp should have been updated because the tab had no timestamp.
                    verify(testSetup.tabList).setPersistedInfo(
                        eq(tab),
                        actualData.capture(),
                        eq(true)
                    )
                    expectThat(actualData.lastValue).isEqualTo(
                        expectedData.copy(lastActiveMs = actualData.lastValue.lastActiveMs)
                    )
                }
            }
        }
    }

    class TestSetup(
        inactiveTabIds: List<String>,
        inactiveTabTimestamps: List<Long?>,
        activeTabId: String?,
        activeTabNavigationIndex: Int = -1
    ) {
        val tabList: TabList = mock {}
        val cleanCache: () -> Unit = mock()
        val onBlankTabCreated: (Tab) -> Unit = mock()
        val onEmptyTabList: () -> Unit = mock()
        val tabs = mutableSetOf<Tab>()
        val restoreCompletedCallback: () -> Unit = mock()

        private val browser = createMockBrowser(
            inactiveTabIds,
            inactiveTabTimestamps,
            activeTabId,
            activeTabNavigationIndex
        )

        val callback = BrowserRestoreCallbackImpl(
            tabList = tabList,
            browser = browser,
            cleanCache = cleanCache,
            onBlankTabCreated = onBlankTabCreated,
            onEmptyTabList = onEmptyTabList,
            afterRestoreCompleted = restoreCompletedCallback
        )

        private fun createMockBrowser(
            inactiveTabIds: List<String>,
            inactiveTabTimestamps: List<Long?>,
            activeTabId: String?,
            activeTabNavigationIndex: Int = -1
        ): Browser {
            // Create a set of Tabs that the Browser will be managing and returning to callers.
            tabs.clear()
            inactiveTabIds.forEachIndexed { index, tabId ->
                val dataMap = mutableMapOf<String, String>().apply {
                    put(TabInfo.PersistedData.KEY_PARENT_TAB_ID, "parent of tabId")

                    inactiveTabTimestamps[index]?.let {
                        put(TabInfo.PersistedData.KEY_LAST_ACTIVE_MS, it.toString())
                    }
                }
                tabs.add(
                    mock {
                        on { getGuid() } doReturn(tabId)
                        on { getData() } doReturn(dataMap)
                    }
                )
            }

            // For the active tab, add a mock NavigationController to check that it's used correctly.
            activeTabId?.let { id ->
                val mockNavigationController: NavigationController = mock {
                    on { getNavigationListCurrentIndex() }.doReturn(activeTabNavigationIndex)
                }
                val mockActiveTab: Tab = mock {
                    on { getGuid() }.doReturn(id)
                    on { getNavigationController() }.doReturn(mockNavigationController)
                }
                tabs.add(mockActiveTab)
            }

            // Mock out the browser itself to return the set of Tabs we just created.
            val activeTab = tabs.firstOrNull { it.guid == activeTabId }
            return mock {
                on { getTabs() }.doReturn(tabs)
                on { getActiveTab() }.doReturn(activeTab)
            }
        }
    }
}
