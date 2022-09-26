// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid.archived

import android.net.Uri
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.paging.compose.LazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.storage.entities.TabData
import com.neeva.app.ui.toLocalDate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class ProcessedArchivedTabsTest : BaseTest() {
    @Test
    fun addsHeadersBetweenNeighboringItems() {
        val lastActiveTimestamps = listOf(
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(4),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(2),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            null
        )

        val tabDataList = createTabDataList(lastActiveTimestamps)
        val lazyTabs = createLazyPagingItems(tabDataList)
        val processed = ProcessedArchivedTabs(lazyTabs)
        processed.apply {
            expectThat(entries.size).isEqualTo(15)
            expectThat(entries[0].date).isEqualTo(tabDataList[0]!!.lastActiveMs.toLocalDate())
            expectThat(entries[1].tabsIndex).isEqualTo(0)
            expectThat(entries[2].tabsIndex).isEqualTo(1)
            expectThat(entries[3].date).isEqualTo(tabDataList[2]!!.lastActiveMs.toLocalDate())
            expectThat(entries[4].tabsIndex).isEqualTo(2)
            expectThat(entries[5].date).isEqualTo(tabDataList[3]!!.lastActiveMs.toLocalDate())
            expectThat(entries[6].tabsIndex).isEqualTo(3)
            expectThat(entries[7].tabsIndex).isEqualTo(4)
            expectThat(entries[8].tabsIndex).isEqualTo(5)
            expectThat(entries[9].date).isEqualTo(tabDataList[6]!!.lastActiveMs.toLocalDate())
            expectThat(entries[10].tabsIndex).isEqualTo(6)
            expectThat(entries[11].date).isEqualTo(tabDataList[7]!!.lastActiveMs.toLocalDate())
            expectThat(entries[12].tabsIndex).isEqualTo(7)
            expectThat(entries[13].tabsIndex).isEqualTo(8)
            expectThat(entries[14].tabsIndex).isEqualTo(9)

            // Entries shouldn't have both a date and a tab index set.
            expectThat(entries[0].tabsIndex).isNull()
            expectThat(entries[1].date).isNull()
            expectThat(entries[2].date).isNull()
            expectThat(entries[3].tabsIndex).isNull()
            expectThat(entries[4].date).isNull()
            expectThat(entries[5].tabsIndex).isNull()
            expectThat(entries[6].date).isNull()
            expectThat(entries[7].date).isNull()
            expectThat(entries[8].date).isNull()
            expectThat(entries[9].tabsIndex).isNull()
            expectThat(entries[10].date).isNull()
            expectThat(entries[11].tabsIndex).isNull()
            expectThat(entries[12].date).isNull()
            expectThat(entries[13].date).isNull()
            expectThat(entries[14].date).isNull()
        }
    }

    @Test
    fun getTabData() {
        val lastActiveTimestamps = listOf(
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(4),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            null
        )

        val tabDataList = createTabDataList(lastActiveTimestamps)
        val lazyTabs = createLazyPagingItems(tabDataList)
        val processed = ProcessedArchivedTabs(lazyTabs)
        processed.apply {
            expectThat(entries.size).isEqualTo(9)
            expectThat(getTabData(0)).isNull()
            expectThat(getTabData(1)).isEqualTo(tabDataList[0])
            expectThat(getTabData(2)).isEqualTo(tabDataList[1])
            expectThat(getTabData(3)).isNull()
            expectThat(getTabData(4)).isEqualTo(tabDataList[2])
            expectThat(getTabData(5)).isNull()
            expectThat(getTabData(6)).isEqualTo(tabDataList[3])
            expectThat(getTabData(7)).isEqualTo(tabDataList[4])
            expectThat(getTabData(8)).isNull()

            // Getting the data explicitly triggers a load of the item.
            lastActiveTimestamps.indices.forEach {
                verify { lazyTabs[it] }
            }
        }
    }

    @Test
    fun getTabData_indexedOutOfBounds() {
        val lastActiveTimestamps = listOf(
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(4),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            null
        )

        val tabDataList = createTabDataList(lastActiveTimestamps)
        val lazyTabs = createLazyPagingItems(tabDataList)
        val processed = ProcessedArchivedTabs(lazyTabs)

        processed.apply {
            expectThat(entries.size).isEqualTo(9)
            expectThat(getTabData(0)).isNull()
            expectThat(getTabData(1)).isEqualTo(tabDataList[0])
            expectThat(getTabData(2)).isEqualTo(tabDataList[1])
            expectThat(getTabData(3)).isNull()
            expectThat(getTabData(4)).isEqualTo(tabDataList[2])
            expectThat(getTabData(5)).isNull()
            expectThat(getTabData(6)).isEqualTo(tabDataList[3])
            expectThat(getTabData(7)).isEqualTo(tabDataList[4])
            expectThat(getTabData(8)).isNull()

            // Say that the PagedList has been emptied out.
            every { lazyTabs.itemCount } returns 0

            // Confirm that we going out of bounds doesn't cause a crash and returns null.
            expectThat(entries.size).isEqualTo(9)
            entries.indices.forEach { expectThat(getTabData(it)).isNull() }
        }
    }

    @Test
    fun keys() {
        val lastActiveTimestamps = listOf(
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(4),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(2),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            null
        )

        val tabDataList = createTabDataList(lastActiveTimestamps)
        val lazyTabs = createLazyPagingItems(tabDataList)
        val processed = ProcessedArchivedTabs(lazyTabs)
        processed.apply {
            expectThat(entries.size).isEqualTo(15)
            expectThat(key(0)).isEqualTo(entries[0].date)
            expectThat(key(1)).isEqualTo(tabDataList[0]!!.id)
            expectThat(key(2)).isEqualTo(tabDataList[1]!!.id)
            expectThat(key(3)).isEqualTo(entries[3].date)
            expectThat(key(4)).isEqualTo(tabDataList[2]!!.id)
            expectThat(key(5)).isEqualTo(entries[5].date)
            expectThat(key(6)).isEqualTo(tabDataList[3]!!.id)
            expectThat(key(7)).isEqualTo(tabDataList[4]!!.id)
            expectThat(key(8)).isEqualTo(tabDataList[5]!!.id)
            expectThat(key(9)).isEqualTo(entries[9].date)
            expectThat(key(10)).isEqualTo(tabDataList[6]!!.id)
            expectThat(key(11)).isEqualTo(entries[11].date)
            expectThat(key(12)).isEqualTo(tabDataList[7]!!.id)
            expectThat(key(13)).isEqualTo(tabDataList[8]!!.id)
            expectThat(key(14)).isEqualTo(ProcessedArchivedTabs.PagingPlaceholderKey(9))

            // Calculating the key should not trigger loading the item.
            lastActiveTimestamps.indices.forEach {
                verify(exactly = 0) { lazyTabs[it] }
            }
        }
    }

    @Test
    fun spans() {
        val lastActiveTimestamps = listOf(
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(4),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(2),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            null
        )

        val tabDataList = createTabDataList(lastActiveTimestamps)
        val lazyTabs = createLazyPagingItems(tabDataList)
        val processed = ProcessedArchivedTabs(lazyTabs)
        processed.apply {
            val numCellsPerRow = 5
            expectThat(entries.size).isEqualTo(15)
            expectThat(span(0, numCellsPerRow)).isEqualTo(GridItemSpan(numCellsPerRow))
            expectThat(span(1, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(2, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(3, numCellsPerRow)).isEqualTo(GridItemSpan(numCellsPerRow))
            expectThat(span(4, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(5, numCellsPerRow)).isEqualTo(GridItemSpan(numCellsPerRow))
            expectThat(span(6, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(7, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(8, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(9, numCellsPerRow)).isEqualTo(GridItemSpan(numCellsPerRow))
            expectThat(span(10, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(11, numCellsPerRow)).isEqualTo(GridItemSpan(numCellsPerRow))
            expectThat(span(12, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(13, numCellsPerRow)).isEqualTo(GridItemSpan(1))
            expectThat(span(14, numCellsPerRow)).isEqualTo(GridItemSpan(1))

            // Calculating the key should not trigger loading the item.
            lastActiveTimestamps.indices.forEach {
                verify(exactly = 0) { lazyTabs[it] }
            }
        }
    }

    @Test
    fun keepsTrackOfPlaceholderItems() {
        val lastActiveTimestamps = listOf(
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(5),
            TimeUnit.DAYS.toMillis(4),
            TimeUnit.DAYS.toMillis(3),
            null,
            null,
            null
        )

        val tabDataList = createTabDataList(lastActiveTimestamps)
        val lazyTabs = createLazyPagingItems(tabDataList)
        val processed = ProcessedArchivedTabs(lazyTabs)
        processed.apply {
            expectThat(entries.size).isEqualTo(10)
            expectThat(entries[0].date).isEqualTo(tabDataList[0]!!.lastActiveMs.toLocalDate())
            expectThat(entries[1].tabsIndex).isEqualTo(0)
            expectThat(entries[2].tabsIndex).isEqualTo(1)
            expectThat(entries[3].date).isEqualTo(tabDataList[2]!!.lastActiveMs.toLocalDate())
            expectThat(entries[4].tabsIndex).isEqualTo(2)
            expectThat(entries[5].date).isEqualTo(tabDataList[3]!!.lastActiveMs.toLocalDate())
            expectThat(entries[6].tabsIndex).isEqualTo(3)
            expectThat(entries[7].tabsIndex).isEqualTo(4)
            expectThat(entries[8].tabsIndex).isEqualTo(5)
            expectThat(entries[9].tabsIndex).isEqualTo(6)
        }
    }

    private fun createTabDataList(timestamps: List<Long?>) = List(timestamps.size) { index ->
        timestamps[index]?.let { timestamp ->
            TabData(
                id = "tab $index",
                url = Uri.parse("https://www.tab.com/$index"),
                title = "unused $index",
                lastActiveMs = timestamp,
                isArchived = true
            )
        }
    }

    private fun createLazyPagingItems(items: List<TabData?>) = mockk<LazyPagingItems<TabData>> {
        every { itemCount } answers { items.size }
        every { get(any<Int>()) } answers { items[it.invocation.args[0] as Int] }
        every { peek(any()) } answers { items[it.invocation.args[0] as Int] }
    }
}
