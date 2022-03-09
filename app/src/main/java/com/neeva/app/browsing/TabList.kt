package com.neeva.app.browsing

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.chromium.weblayer.Tab

/**
 * Maintains a list of Tabs that are displayed in the browser, as well as the metadata associated
 * with each.
 */
class TabList {
    private val tabs: MutableList<Tab> = mutableListOf()
    private val tabInfoMap: MutableMap<String, TabInfo> = mutableMapOf()

    private val _orderedTabList = MutableStateFlow<List<TabInfo>>(emptyList())
    val orderedTabList: StateFlow<List<TabInfo>> = _orderedTabList
    val hasNoTabsFlow: Flow<Boolean> = _orderedTabList.map { it.isEmpty() }

    fun hasNoTabs(): Boolean = tabs.isEmpty()
    fun indexOf(tab: Tab) = tabs.indexOf(tab)
    fun findTab(id: String) = tabs.firstOrNull { it.guid == id }
    fun getTab(index: Int) = tabs[index]
    fun getTabInfo(id: String) = tabInfoMap[id]

    fun add(tab: Tab) {
        if (tabs.contains(tab)) return
        tabs.add(tab)

        tabInfoMap[tab.guid] = TabInfo(
            id = tab.guid,
            url = tab.currentDisplayUrl,
            title = tab.currentDisplayTitle,
            isSelected = tab.isSelected,
            data = TabInfo.PersistedData(tab.data)
        )

        updateFlow()
    }

    /** Removes the given Tab from the list.  If we had a corresponding TabInfo, it is returned. */
    fun remove(tab: Tab): TabInfo? {
        tabs.remove(tab)
        val childInfo = tabInfoMap.remove(tab.guid)
        updateFlow()
        return childInfo
    }

    fun updatedSelectedTab(selectedTabId: String?) {
        tabInfoMap.keys.forEach { tabId ->
            val newSelectedValue = selectedTabId != null && tabId == selectedTabId
            tabInfoMap[tabId]?.let { currentData ->
                if (currentData.isSelected != newSelectedValue) {
                    tabInfoMap[tabId] = currentData.copy(
                        isSelected = newSelectedValue
                    )
                }
            }
        }

        updateFlow()
    }

    fun updateTabTitle(tabId: String, newTitle: String?) {
        tabInfoMap[tabId]
            ?.takeUnless { it.title == newTitle }
            ?.let { existingInfo ->
                tabInfoMap[tabId] = existingInfo.copy(title = newTitle)
                updateFlow()
            }
    }

    fun updateUrl(tabId: String, newUrl: Uri?) {
        tabInfoMap[tabId]
            ?.takeUnless { it.url == newUrl }
            ?.let { existingInfo ->
                tabInfoMap[tabId] = existingInfo.copy(url = newUrl)
                updateFlow()
            }
    }

    fun updateIsCrashed(tabId: String, isCrashed: Boolean) {
        tabInfoMap[tabId]
            ?.takeUnless { it.isCrashed == isCrashed }
            ?.let { existingInfo ->
                tabInfoMap[tabId] = existingInfo.copy(isCrashed = isCrashed)
                updateFlow()
            }
    }

    fun updateParentInfo(tab: Tab, parentTabId: String?, tabOpenType: TabInfo.TabOpenType) {
        setPersistedInfo(
            tab = tab,
            newData = TabInfo.PersistedData(
                parentTabId = parentTabId,
                openType = tabOpenType
            ),
            persist = true
        )
    }

    internal fun setPersistedInfo(tab: Tab, newData: TabInfo.PersistedData, persist: Boolean) {
        if (tab.isDestroyed) return

        val tabId = tab.guid
        tabInfoMap[tabId]
            ?.takeUnless { it.data == newData }
            ?.let { existingInfo ->
                val newInfo = existingInfo.copy(data = newData)
                tabInfoMap[tabId] = newInfo
                updateFlow()

                // Save data out to the Tab structure so that WebLayer can restore it for us later.
                if (persist) tab.data = newInfo.data.toMap()
            }
    }

    internal fun clear() {
        tabs.clear()
        tabInfoMap.clear()
        updateFlow()
    }

    private fun updateFlow() {
        _orderedTabList.value = tabs.map { tabInfoMap[it.guid]!! }
    }

    internal fun forEach(closure: (Tab) -> Unit) {
        tabs.forEach(action = closure)
    }
}
