// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage

import android.net.Uri
import com.apollographql.apollo3.testing.runTest
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.storage.daos.SearchNavigationDao
import com.neeva.app.storage.entities.SearchNavigation
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

@HiltAndroidTest
class SearchNavigationDaoTest : HistoryDatabaseBaseTest() {
    private lateinit var searchNavigationDao: SearchNavigationDao

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    override fun setUp() {
        super.setUp()
        searchNavigationDao = database.searchNavigationDao()
    }

    @Test
    fun add() = runTest {
        searchNavigationDao.add(TAB_1_QUERY_1)
        searchNavigationDao.add(TAB_1_QUERY_2)
        searchNavigationDao.add(TAB_2_QUERY_1)

        expectThat(searchNavigationDao.getAllForTab("tab id")).containsExactly(
            TAB_1_QUERY_1,
            TAB_1_QUERY_2
        )
        expectThat(searchNavigationDao.getAllForTab("tab id 2")).containsExactly(
            TAB_2_QUERY_1
        )

        // Replace one of the entries in the table.
        searchNavigationDao.add(TAB_1_QUERY_3)
        expectThat(searchNavigationDao.getAllForTab("tab id")).containsExactly(
            TAB_1_QUERY_3,
            TAB_1_QUERY_2
        )
    }

    @Test
    fun delete() = runTest {
        searchNavigationDao.add(TAB_1_QUERY_1)
        searchNavigationDao.add(TAB_1_QUERY_2)

        searchNavigationDao.delete(
            tabId = "tab id",
            navigationEntryIndex = TAB_1_QUERY_1.navigationEntryIndex
        )

        expectThat(searchNavigationDao.getAllForTab("tab id")).containsExactly(
            TAB_1_QUERY_2
        )
    }

    @Test
    fun prune() = runTest {
        searchNavigationDao.add(TAB_1_QUERY_1)
        searchNavigationDao.add(TAB_1_QUERY_2)
        searchNavigationDao.add(TAB_2_QUERY_1)

        searchNavigationDao.prune(tabId = "tab id", maxNavigationEntryIndex = 6)

        expectThat(searchNavigationDao.getAllForTab("tab id")).containsExactly(TAB_1_QUERY_1)
        expectThat(searchNavigationDao.getAllForTab("tab id 2")).containsExactly(TAB_2_QUERY_1)
    }

    @Test
    fun deleteAllForTab() = runTest {
        searchNavigationDao.add(TAB_1_QUERY_1)
        searchNavigationDao.add(TAB_1_QUERY_2)
        searchNavigationDao.add(
            SearchNavigation(
                tabId = "tab id 2",
                navigationEntryIndex = 9,
                navigationEntryUri = Uri.parse("http://hn.premii.com"),
                searchQuery = "tab 2 query 1"
            )
        )

        searchNavigationDao.deleteAllForTab(tabId = "tab id")

        expectThat(searchNavigationDao.getAllForTab("tab id")).isEmpty()
        expectThat(searchNavigationDao.getAllForTab("tab id 2")).containsExactly(
            SearchNavigation(
                tabId = "tab id 2",
                navigationEntryIndex = 9,
                navigationEntryUri = Uri.parse("http://hn.premii.com"),
                searchQuery = "tab 2 query 1"
            )
        )
    }

    @Test
    fun deleteAllForTabs() = runTest {
        searchNavigationDao.add(TAB_1_QUERY_1)
        searchNavigationDao.add(TAB_1_QUERY_2)
        searchNavigationDao.add(TAB_2_QUERY_1)
        searchNavigationDao.add(
            SearchNavigation(
                tabId = "tab id 3",
                navigationEntryIndex = 1,
                navigationEntryUri = Uri.parse("http://hn.premii.com"),
                searchQuery = "tab 3 query 1"
            )
        )

        searchNavigationDao.deleteAllForTabs(listOf("tab id", "tab id 2"))

        expectThat(searchNavigationDao.getAllForTab("tab id")).isEmpty()
        expectThat(searchNavigationDao.getAllForTab("tab id 2")).isEmpty()
        expectThat(searchNavigationDao.getAllForTab("tab id 3")).containsExactly(
            SearchNavigation(
                tabId = "tab id 3",
                navigationEntryIndex = 1,
                navigationEntryUri = Uri.parse("http://hn.premii.com"),
                searchQuery = "tab 3 query 1"
            )
        )
    }

    @Test
    fun listToMap() {
        val map = SearchNavigationDao.listToMap(listOf(TAB_1_QUERY_1, TAB_1_QUERY_2, TAB_2_QUERY_1))
        expectThat(map["tab id"]!!).containsExactly(TAB_1_QUERY_1, TAB_1_QUERY_2)
        expectThat(map["tab id 2"]!!).containsExactly(TAB_2_QUERY_1)
    }

    companion object {
        private val TAB_1_QUERY_1 = SearchNavigation(
            tabId = "tab id",
            navigationEntryIndex = 5,
            navigationEntryUri = Uri.parse("http://www.reddit.com"),
            searchQuery = "query 1"
        )
        private val TAB_1_QUERY_2 = SearchNavigation(
            tabId = "tab id",
            navigationEntryIndex = 8,
            navigationEntryUri = Uri.parse("http://www.youtube.com"),
            searchQuery = "query 2"
        )
        private val TAB_1_QUERY_3 = SearchNavigation(
            tabId = "tab id",
            navigationEntryIndex = 5,
            navigationEntryUri = Uri.parse("http://neeva.com"),
            searchQuery = "tab 1 query 3"
        )

        private val TAB_2_QUERY_1 = SearchNavigation(
            tabId = "tab id 2",
            navigationEntryIndex = 9,
            navigationEntryUri = Uri.parse("http://hn.premii.com"),
            searchQuery = "tab 2 query 1"
        )
    }
}
