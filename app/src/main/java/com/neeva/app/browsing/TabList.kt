// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.Dispatchers
import com.neeva.app.storage.daos.SearchNavigationDao
import com.neeva.app.storage.entities.SearchNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab

/**
 * Maintains a list of Tabs that are displayed in the browser, as well as the metadata associated
 * with each.
 */
abstract class TabList {
    protected val tabs: MutableList<String> = mutableListOf()
    private val tabInfoMap: MutableMap<String, TabInfo> = mutableMapOf()
    private val fuzzyMatchMap: MutableMap<UriFuzzyMatchData, MutableSet<String>> = mutableMapOf()

    private val _orderedTabList = MutableStateFlow<List<TabInfo>>(emptyList())
    val orderedTabList: StateFlow<List<TabInfo>> = _orderedTabList

    abstract val searchNavigationMap: StateFlow<Map<String, List<SearchNavigation>>>?

    /**
     * Returns true if there are no open tabs in the list.
     *
     * @param ignoreClosingTabs If true, any tabs that are being closed are treated as already gone.
     */
    fun hasNoTabs(ignoreClosingTabs: Boolean): Boolean {
        val filteredList = if (ignoreClosingTabs) {
            tabs.filterNot { tabInfoMap[it]?.isClosing == true }
        } else {
            tabs
        }
        return filteredList.isEmpty()
    }

    fun indexOf(id: String) = tabs.indexOf(id)
    fun getTabInfo(id: String) = tabInfoMap[id]

    fun add(tab: Tab) {
        if (tabs.contains(tab.guid)) return
        tabs.add(tab.guid)

        TabInfo(
            id = tab.guid,
            url = tab.currentDisplayUrl,
            title = tab.currentDisplayTitle,
            isSelected = tab.isSelected,
            data = TabInfo.PersistedData(tab.isSelected, tab.data)
        ).apply {
            tabInfoMap[tab.guid] = this

            fuzzyMatchUrl?.let {
                val updatedSet = fuzzyMatchMap.getOrDefault(it, mutableSetOf())
                updatedSet.add(id)
                fuzzyMatchMap[it] = updatedSet
            }
        }

        updateFlow()
    }

    /** Removes the given Tab from the list.  If we had a corresponding TabInfo, it is returned. */
    fun remove(tabId: String): TabInfo? {
        tabs.remove(tabId)
        val childInfo = tabInfoMap.remove(tabId)
        childInfo?.fuzzyMatchUrl?.let { fuzzyMatchMap[it]?.remove(tabId) }
        updateFlow()
        removeQueryNavigations(tabId)
        return childInfo
    }

    /** Records the query that triggered a navigation via Search As You Type. */
    abstract fun updateQueryNavigation(
        tabId: String,
        navigationEntryIndex: Int,
        navigationEntryUri: Uri,
        searchQuery: String?
    )

    /** Removes all query records associated with the given [tabId]. */
    abstract fun removeQueryNavigations(tabId: String)

    /** Removes any query records that correspond to tabs that no longer exist. */
    abstract fun pruneQueryNavigations()

    /** Removes any recorded search queries that correspond to Navigations that no longer exist. */
    abstract fun pruneQueryNavigations(tabId: String, navigationController: NavigationController)

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
                val newInfo = existingInfo.copy(url = newUrl)
                tabInfoMap[tabId] = newInfo

                existingInfo.fuzzyMatchUrl?.let {
                    fuzzyMatchMap[it]?.remove(existingInfo.id)
                }
                newInfo.fuzzyMatchUrl?.let {
                    val updatedSet = fuzzyMatchMap.getOrDefault(it, mutableSetOf())
                    updatedSet.add(newInfo.id)
                    fuzzyMatchMap[it] = updatedSet
                }

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

    fun updateIsClosing(tabId: String, newIsClosing: Boolean) {
        tabInfoMap[tabId]
            ?.takeUnless { it.isClosing == newIsClosing }
            ?.let { existingInfo ->
                tabInfoMap[tabId] = existingInfo.copy(isClosing = newIsClosing)
                updateFlow()
            }
    }

    fun updateIsPinned(tab: Tab, isPinned: Boolean) {
        if (tab.isDestroyed) return

        tabInfoMap[tab.guid]?.let { existingTabInfo ->
            setPersistedInfo(
                tab = tab,
                newData = existingTabInfo.data.copy(
                    isPinned = isPinned
                ),
                persist = true
            )
        }
    }

    fun updateParentInfo(
        tab: Tab,
        parentTabId: String?,
        parentSpaceId: String?,
        tabOpenType: TabInfo.TabOpenType
    ) {
        if (tab.isDestroyed) return

        tabInfoMap[tab.guid]?.let { existingTabInfo ->
            setPersistedInfo(
                tab = tab,
                newData = existingTabInfo.data.copy(
                    parentTabId = parentTabId,
                    parentSpaceId = parentSpaceId,
                    openType = tabOpenType
                ),
                persist = true
            )
        }
    }

    internal fun updateTimestamp(tab: Tab, lastActiveTimestamp: Long) {
        if (tab.isDestroyed) return

        tabInfoMap[tab.guid]?.let { existingTabInfo ->
            setPersistedInfo(
                tab = tab,
                newData = existingTabInfo.data.copy(
                    lastActiveMs = lastActiveTimestamp
                ),
                persist = true
            )
        }
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

    internal fun isParentTabInList(tabId: String?): Boolean {
        if (tabId == null) return false

        val tabInfo = getTabInfo(tabId)
        return tabInfo?.data
            ?.takeIf { it.openType == TabInfo.TabOpenType.CHILD_TAB }
            ?.let { data -> data.parentTabId?.let { tabs.contains(it) } }
            ?: false
    }

    internal fun clear() {
        tabs.clear()
        tabInfoMap.clear()
        fuzzyMatchMap.clear()
        updateFlow()
    }

    /** Returns the ID of a pre-existing Tab with a URI similar to the [uri] being passed in. */
    fun findTabWithSimilarUri(uri: Uri): String? {
        val fuzzyUri = UriFuzzyMatchData.create(uri) ?: return null
        return fuzzyMatchMap[fuzzyUri]?.firstOrNull()
    }

    fun getSearchNavigationInfo(guid: String, navigationEntryIndex: Int): SearchNavigation? {
        return searchNavigationMap?.value
            ?.get(guid)
            ?.firstOrNull { it.navigationEntryIndex == navigationEntryIndex }
    }

    private fun updateFlow() {
        _orderedTabList.value = tabs.mapNotNull { guid -> tabInfoMap[guid] }
    }

    internal fun forEach(closure: (String) -> Unit) {
        tabs.forEach(action = closure)
    }
}

/** TabList that does not persist its data to storage. */
class IncognitoTabList : TabList() {
    override val searchNavigationMap =
        MutableStateFlow<MutableMap<String, MutableList<SearchNavigation>>>(mutableMapOf())

    override fun updateQueryNavigation(
        tabId: String,
        navigationEntryIndex: Int,
        navigationEntryUri: Uri,
        searchQuery: String?
    ) {
        searchNavigationMap.value = searchNavigationMap.value.apply {
            val tabEntries = getOrDefault(tabId, mutableListOf())
            if (searchQuery != null) {
                tabEntries.add(
                    SearchNavigation(
                        tabId = tabId,
                        navigationEntryIndex = navigationEntryIndex,
                        navigationEntryUri = navigationEntryUri,
                        searchQuery = searchQuery
                    )
                )
            } else {
                tabEntries
                    .firstOrNull { it.navigationEntryIndex == navigationEntryIndex }
                    ?.let { tabEntries.remove(it) }
            }
            put(tabId, tabEntries)
        }
    }

    override fun removeQueryNavigations(tabId: String) {
        searchNavigationMap.value.remove(tabId)
    }

    override fun pruneQueryNavigations() {
        val liveTabIds = tabs.toSet()
        val allTabIds = searchNavigationMap.value.keys
        allTabIds.minus(liveTabIds).forEach {
            removeQueryNavigations(it)
        }
    }

    override fun pruneQueryNavigations(tabId: String, navigationController: NavigationController) {
        val maxNavigationIndex = navigationController.navigationListSize
        searchNavigationMap.value = searchNavigationMap.value.apply {
            get(tabId)?.let { entries ->
                put(
                    tabId,
                    entries
                        .filter { it.navigationEntryIndex < maxNavigationIndex }
                        .toMutableList()
                )
            }
        }
    }
}

class RegularTabList(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val searchNavigationDao: SearchNavigationDao
) : TabList() {
    override val searchNavigationMap = searchNavigationDao.getAllMapFlow(
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    )

    override fun updateQueryNavigation(
        tabId: String,
        navigationEntryIndex: Int,
        navigationEntryUri: Uri,
        searchQuery: String?
    ) {
        coroutineScope.launch(dispatchers.io) {
            if (searchQuery != null) {
                searchNavigationDao.add(
                    SearchNavigation(
                        tabId = tabId,
                        navigationEntryIndex = navigationEntryIndex,
                        navigationEntryUri = navigationEntryUri,
                        searchQuery = searchQuery
                    )
                )
            } else {
                searchNavigationDao.delete(
                    tabId = tabId,
                    navigationEntryIndex = navigationEntryIndex
                )
            }
        }
    }

    override fun removeQueryNavigations(tabId: String) {
        coroutineScope.launch(dispatchers.io) {
            searchNavigationDao.deleteAllForTab(tabId)
        }
    }

    override fun pruneQueryNavigations() {
        val liveTabIds = tabs.toSet()
        val allTabIds = searchNavigationMap.value.keys
        coroutineScope.launch(dispatchers.io) {
            val missingTabs = allTabIds.minus(liveTabIds).toList()
            searchNavigationDao.deleteAllForTabs(missingTabs)
        }
    }

    override fun pruneQueryNavigations(tabId: String, navigationController: NavigationController) {
        val size = navigationController.navigationListSize
        coroutineScope.launch(dispatchers.io) {
            searchNavigationDao.prune(tabId, size)
        }
    }
}
