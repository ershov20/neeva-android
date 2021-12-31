package com.neeva.app.browsing

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Tab

/**
 * Maintains a list of Tabs that are displayed in the browser, as well as the metadata associated
 * with each.
 */
class TabList {
    private val currentTabs: MutableList<Tab> = mutableListOf()
    private val currentPrimitives: MutableMap<String, TabInfo> = mutableMapOf()

    private val _orderedTabList = MutableStateFlow<List<TabInfo>>(emptyList())
    val orderedTabList: StateFlow<List<TabInfo>> = _orderedTabList

    fun indexOf(tab: Tab) = currentTabs.indexOf(tab)
    fun findTab(id: String) = currentTabs.firstOrNull { it.guid == id }
    fun getTab(index: Int) = currentTabs[index]
    fun getTabInfo(id: String) = currentPrimitives[id]

    fun add(tab: Tab) {
        if (currentTabs.contains(tab)) return
        currentTabs.add(tab)

        currentPrimitives[tab.guid] = TabInfo(
            id = tab.guid,
            url = tab.currentDisplayUrl,
            title = tab.currentDisplayTitle,
            isSelected =  tab.isSelected
        )

        updateFlow()
    }

    /** Removes the given Tab from the list.  If we had a corresponding TabInfo, it is returned. */
    fun remove(tab: Tab): TabInfo? {
        currentTabs.remove(tab)
        val childInfo = currentPrimitives.remove(tab.guid)
        updateFlow()
        return childInfo
    }

    fun updatedSelectedTab(selectedTabId: String?) {
        currentPrimitives.keys.forEach { tabId ->
            val newSelectedValue = selectedTabId != null && tabId == selectedTabId
            currentPrimitives[tabId]?.let { currentData ->
                if (currentData.isSelected != newSelectedValue) {
                    currentPrimitives[tabId] = currentData.copy(
                        isSelected = newSelectedValue
                    )
                }
            }
        }

        updateFlow()
    }

    fun updateTabTitle(tabId: String, newTitle: String?) {
        val existingTab = currentPrimitives[tabId] ?: return
        if (existingTab.title == newTitle) return
        currentPrimitives[tabId] = existingTab.copy(title = newTitle)
        updateFlow()
    }

    fun updateUrl(tabId: String, newUrl: Uri?) {
        val existingTab = currentPrimitives[tabId] ?: return
        if (existingTab.url == newUrl) return
        currentPrimitives[tabId] = existingTab.copy(url = newUrl)
        updateFlow()
    }

    fun updateThumbnailUri(tabId: String, newThumbnailUri: Uri?) {
        val existingTab = currentPrimitives[tabId] ?: return
        if (existingTab.thumbnailUri == newThumbnailUri) return
        currentPrimitives[tabId] = existingTab.copy(thumbnailUri = newThumbnailUri)
        updateFlow()
    }

    fun updateParentTabId(tabId: String, parentTabId: String?) {
        val existingTab = currentPrimitives[tabId] ?: return
        if (existingTab.parentTabId == parentTabId) return
        currentPrimitives[tabId] = existingTab.copy(parentTabId = parentTabId)
        updateFlow()
    }

    private fun updateFlow() {
        _orderedTabList.value = currentTabs.map { currentPrimitives[it.guid]!! }
    }
}
