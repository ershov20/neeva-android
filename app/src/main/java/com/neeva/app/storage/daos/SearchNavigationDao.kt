// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neeva.app.Dispatchers
import com.neeva.app.storage.entities.SearchNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Dao
interface SearchNavigationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(entry: SearchNavigation)

    @Query(
        """
        DELETE
        FROM SearchNavigation
        WHERE tabId = :tabId AND navigationEntryIndex = :navigationEntryIndex
        """
    )
    fun delete(tabId: String, navigationEntryIndex: Int)

    /** Removes any entries that correspond to navigations that no longer exist. */
    @Query(
        """
        DELETE
        FROM SearchNavigation
        WHERE tabId = :tabId AND navigationEntryIndex >= :maxNavigationEntryIndex
        """
    )
    fun prune(tabId: String, maxNavigationEntryIndex: Int)

    @Query(
        """
        SELECT * 
        FROM SearchNavigation
        WHERE tabId = :tabId
        """
    )
    fun getAllForTab(tabId: String): List<SearchNavigation>

    @Query("SELECT * FROM SearchNavigation")
    fun getAllFlow(): Flow<List<SearchNavigation>>

    @Query(
        """
        DELETE
        FROM SearchNavigation
        WHERE tabId = :tabId
        """
    )
    fun deleteAllForTab(tabId: String)

    @Query(
        """
        DELETE
        FROM SearchNavigation
        WHERE tabId IN (:tabIds)
        """
    )
    fun deleteAllForTabs(tabIds: List<String>)

    companion object {
        fun listToMap(
            list: List<SearchNavigation>
        ): MutableMap<String, MutableList<SearchNavigation>> {
            return mutableMapOf<String, MutableList<SearchNavigation>>().apply {
                list.forEach {
                    put(
                        it.tabId,
                        getOrDefault(it.tabId, mutableListOf()).apply { add(it) }
                    )
                }
            }
        }
    }

    fun getAllMapFlow(coroutineScope: CoroutineScope, dispatchers: Dispatchers) = getAllFlow()
        .distinctUntilChanged()
        .map(SearchNavigationDao::listToMap)
        .flowOn(dispatchers.io)
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())
}
