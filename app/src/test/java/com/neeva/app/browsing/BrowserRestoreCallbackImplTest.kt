package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.NeevaConstants
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BrowserRestoreCallbackImplTest {
    @Test
    fun onRestoreCompleted_withoutTabs_firesEmptyTabList() {
        // Arrange: Say that the browser had no tabs.
        val testSetup = TestSetup(emptyList(), null)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: We should have received a callback about there being no tabs to restore.
        verify(testSetup.onEmptyTabList, times(1)).invoke()

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_withSingleActiveTabAndInvalidNavigation_goesHome() {
        // Arrange: Say that the browser had only one tab, it was active, and in a bad state.
        val activeTabId = "tab b"
        val testSetup = TestSetup(emptyList(), activeTabId, -1)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: It should have navigated the active tab.
        val uriCaptor = argumentCaptor<Uri>()
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.browser.activeTab!!.navigationController, times(1))
            .navigate(uriCaptor.capture())
        expectThat(uriCaptor.lastValue).isEqualTo(Uri.parse(NeevaConstants.appURL))

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_withSingleActiveTabAndValidNavigation_staysPut() {
        // Arrange: Say that the browser had only one tab, it was active, and in a good state.
        val activeTabId = "tab b"
        val testSetup = TestSetup(emptyList(), activeTabId, 13)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: It should have not navigated anywhere.
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.browser.activeTab!!.navigationController, never()).navigate(any())

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_withActiveTabAndInvalidNavigation_doesNothing() {
        // Arrange: Say that the browser had three tabs and that the active tab was in a bad state.
        val testSetup = TestSetup(listOf("tab a", "tab c"), "tab b")

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: Because there were multiple tabs restored, navigation logic shouldn't kick in.
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.browser.activeTab!!.navigationController, never()).navigate(any())

        verify(testSetup.cleanCache, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_restoresTabData() {
        // Arrange: Say that the browser had three tabs and that the active tab was in a bad state.
        val testSetup = TestSetup(listOf("tab a", "tab c"), "tab b")

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: Confirm that the data was pulled back out correctly.
        testSetup.tabs.forEach {
            val expectedData = TabInfo.PersistedData(it.data)
            verify(testSetup.tabList).setPersistedInfo(eq(it), eq(expectedData), eq(false))
        }
    }

    class TestSetup(
        inactiveTabIds: List<String>,
        activeTabId: String?,
        activeTabNavigationIndex: Int = -1
    ) {
        val tabList: TabList = mock {}
        val cleanCache: () -> Unit = mock()
        val onEmptyTabList: () -> Unit = mock()
        val tabs = mutableSetOf<Tab>()

        val browser = createMockBrowser(inactiveTabIds, activeTabId, activeTabNavigationIndex)

        val callback = BrowserRestoreCallbackImpl(
            tabList = tabList,
            browser = browser,
            cleanCache = cleanCache,
            onEmptyTabList = onEmptyTabList
        )

        private fun createMockBrowser(
            inactiveTabIds: List<String>,
            activeTabId: String?,
            activeTabNavigationIndex: Int = -1
        ): Browser {
            // Create a set of Tabs that the Browser will be managing and returning to callers.
            tabs.clear()
            inactiveTabIds.forEach { tabId ->
                val dataMap = mapOf(TabInfo.KEY_PARENT_TAB_ID to "parent of tabId")
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
