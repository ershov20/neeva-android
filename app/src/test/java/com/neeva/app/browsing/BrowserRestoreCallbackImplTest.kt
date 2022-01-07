package com.neeva.app.browsing

import android.net.Uri
import android.os.Bundle
import com.neeva.app.NeevaConstants
import org.chromium.weblayer.Browser
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BrowserRestoreCallbackImplTest {
    @Test
    fun onRestoreCompleted_withoutTabs_firesEmptyTabList() {
        // Arrange: Say that the browser had no tabs.
        val testSetup = TestSetup(null, emptyList(), null)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: We should have received a callback about there being no tabs to restore.
        expectThat(testSetup.addedTabs).isEmpty()
        verify(testSetup.onEmptyTabList, times(1)).invoke()
    }

    @Test
    fun onRestoreCompleted_withoutSavedInstanceState_restoresNoTabs() {
        // Arrange: Say that the browser had three tabs but we didn't persist any to the Bundle.
        val tabIds = listOf("tab a", "tab b", "tab c")
        val testSetup = TestSetup(null, tabIds, null)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: No tabs were specified in the bundle so nothing should be restored.
        expectThat(testSetup.addedTabs.map { it.guid }).isEmpty()
    }

    @Test
    fun onRestoreCompleted_withSavedInstanceState_restoresNamedTabs() {
        // Arrange: Say that the browser had three tabs but we only knew about two of them.
        val tabIds = listOf("tab a", "tab b", "tab c")
        val restoredTabIds = listOf("tab b", "tab c")
        val lastSavedInstanceState = Bundle()
        BrowserRestoreCallbackImpl.onSaveInstanceState(lastSavedInstanceState, restoredTabIds)
        val testSetup = TestSetup(lastSavedInstanceState, tabIds, null)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: We should only have restored the tabs we specified.
        expectThat(testSetup.addedTabs.map { it.guid }).containsExactlyInAnyOrder(restoredTabIds)
        verify(testSetup.onEmptyTabList, times(0)).invoke()
    }

    @Test
    fun onRestoreCompleted_withSingleActiveTabAndInvalidNavigation_goesHome() {
        // Arrange: Say that the browser had only one tab, it was active, and in a bad state.
        val activeTabId = "tab b"
        val lastSavedInstanceState = Bundle()
        BrowserRestoreCallbackImpl.onSaveInstanceState(lastSavedInstanceState, listOf(activeTabId))
        val testSetup = TestSetup(lastSavedInstanceState, emptyList(), activeTabId, -1)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: All tabs should have been restored and it should have navigated the active tab.
        val uriCaptor = argumentCaptor<Uri>()
        expectThat(testSetup.addedTabs.map { it.guid }).containsExactlyInAnyOrder(activeTabId)
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.browser.activeTab!!.navigationController, times(1))
            .navigate(uriCaptor.capture())
        expectThat(uriCaptor.lastValue).isEqualTo(Uri.parse(NeevaConstants.appURL))
    }

    @Test
    fun onRestoreCompleted_withSingleActiveTabAndValidNavigation_staysPut() {
        // Arrange: Say that the browser had only one tab, it was active, and in a good state.
        val activeTabId = "tab b"
        val lastSavedInstanceState = Bundle()
        BrowserRestoreCallbackImpl.onSaveInstanceState(lastSavedInstanceState, listOf(activeTabId))
        val testSetup = TestSetup(lastSavedInstanceState, emptyList(), activeTabId, 13)

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: All tabs should have been restored and it should have not navigated anywhere.
        expectThat(testSetup.addedTabs.map { it.guid }).containsExactlyInAnyOrder(activeTabId)
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.browser.activeTab!!.navigationController, never()).navigate(any())
    }

    @Test
    fun onRestoreCompleted_withActiveTabAndInvalidNavigation_doesNothing() {
        // Arrange: Say that the browser had three tabs and that the active tab was in a bad state.
        val tabIds = listOf("tab a", "tab b", "tab c")

        // Allow all the tabs to be restored.
        val lastSavedInstanceState = Bundle()
        BrowserRestoreCallbackImpl.onSaveInstanceState(lastSavedInstanceState, tabIds)
        val testSetup = TestSetup(lastSavedInstanceState, listOf("tab a", "tab c"), "tab b")

        // Act: Restore the tabs.
        testSetup.callback.onRestoreCompleted()

        // Assert: Because there were multiple tabs restored, navigation logic shouldn't kick in.
        expectThat(testSetup.addedTabs.map { it.guid }).containsExactlyInAnyOrder(tabIds)
        verify(testSetup.onEmptyTabList, times(0)).invoke()
        verify(testSetup.browser.activeTab!!.navigationController, never()).navigate(any())
    }

    class TestSetup(
        lastSavedInstanceState: Bundle?,
        inactiveTabIds: List<String>,
        activeTabId: String?,
        activeTabNavigationIndex: Int = -1
    ) {
        val onEmptyTabList: () -> Unit = mock()

        val addedTabs = mutableSetOf<Tab>()

        val browser = createMockBrowser(inactiveTabIds, activeTabId, activeTabNavigationIndex)

        val callback = BrowserRestoreCallbackImpl(
            lastSavedInstanceState = lastSavedInstanceState,
            browser = browser,
            onEmptyTabList = onEmptyTabList,
            onNewTabAdded = { addedTabs.add(it) }
        )

        private fun createMockBrowser(
            inactiveTabIds: List<String>,
            activeTabId: String?,
            activeTabNavigationIndex: Int = -1
        ): Browser {
            // Create a set of Tabs that the Browser will be managing and returning to callers.
            val tabs = mutableSetOf<Tab>()
            inactiveTabIds.forEach { tabId ->
                tabs.add(
                    mock {
                        on { getGuid() }.doReturn(tabId)
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
