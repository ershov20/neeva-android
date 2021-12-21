package com.neeva.app.browsing

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import org.chromium.weblayer.Tab

/**
 * Maintains a list of Tabs that are displayed in the browser, as well as the metadata associated
 * with each.
 */
class TabList {
    private val currentTabs: MutableList<Tab> = mutableListOf()
    private val currentPrimitives: MutableMap<String, BrowserPrimitive> = mutableMapOf()

    val orderedTabList = MutableLiveData<List<BrowserPrimitive>>()

    fun indexOf(tab: Tab) = currentTabs.indexOf(tab)
    fun findTab(id: String) = currentTabs.firstOrNull { it.guid == id }
    fun getTab(index: Int) = currentTabs[index]

    fun add(tab: Tab) {
        currentTabs.add(tab)

        currentPrimitives[tab.guid] = BrowserPrimitive(
            id = tab.guid,
            url = tab.currentDisplayUrl,
            title = tab.currentDisplayTitle,
            isSelected =  tab.isSelected
        )

        updateLiveData()
    }

    fun remove(tab: Tab) {
        currentTabs.remove(tab)
        currentPrimitives.remove(tab.guid)
        updateLiveData()
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

        updateLiveData()
    }

    fun updateTabTitle(tabId: String, newTitle: String?) {
        val existingTab = currentPrimitives[tabId] ?: return
        if (existingTab.title == newTitle) return
        currentPrimitives[tabId] = existingTab.copy(title = newTitle)
        updateLiveData()
    }

    fun updateUrl(tabId: String, newUrl: Uri?) {
        val existingTab = currentPrimitives[tabId] ?: return
        if (existingTab.url == newUrl) return
        currentPrimitives[tabId] = existingTab.copy(url = newUrl)
        updateLiveData()
    }

    fun updateThumbnailUri(tabId: String, newThumbnailUri: Uri?) {
        val existingTab = currentPrimitives[tabId] ?: return
        if (existingTab.thumbnailUri == newThumbnailUri) return
        currentPrimitives[tabId] = existingTab.copy(thumbnailUri = newThumbnailUri)
        updateLiveData()
    }

    private fun updateLiveData() {
        orderedTabList.value = currentTabs.map { currentPrimitives[it.guid]!! }
    }
}