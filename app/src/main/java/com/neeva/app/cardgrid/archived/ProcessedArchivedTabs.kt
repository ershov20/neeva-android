// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid.archived

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.paging.compose.LazyPagingItems
import com.neeva.app.storage.entities.TabData
import com.neeva.app.ui.toLocalDate
import java.time.LocalDate

/**
 * Processes the provided [tabs] for display, adding additional items in the list for date headers
 * whenever two neighboring [TabData] instances correspond to two different dates.
 *
 * [tabs] is expected to provide items in a sorted chronological order.
 */
internal class ProcessedArchivedTabs(private val tabs: LazyPagingItems<TabData>) {
    data class ArchivedTabsListEntry(
        /** If non-null: Item that this object corresponds to in the [tabs]. */
        val date: LocalDate? = null,

        /** If non-null: Index of the [TabData] that this corresponds to in the [tabs]. */
        val tabsIndex: Int? = null
    )

    /**
     * Processes the [tabs] and add extra entries representing date headers between two instances
     * that were last active on different days.
     *
     * Entries that correspond to the original tabs are stored as indices into the original [tabs]
     * list as [ArchivedTabsListEntry.tabsIndex].  To get the original [TabData] corresponding to
     * entry, use [getTabData].
     *
     * EXAMPLE
     * If the constructor is provided these 3 [tabs]:
     * - TabData 0: Active 2022/09/10
     * - TabData 1: Active 2022/09/10
     * - TabData 2: Active 2022/09/03
     *
     * [entries] will contain the following 5 [ArchivedTabsListEntry]s:
     * - 0: Date header for 2022/09/10
     * - 1: Pointer to TabData 0
     * - 2: Pointer to TabData 1
     * - 3: Date header for 2022/09/03
     * - 4: Pointer to TabData 2
     */
    val entries: List<ArchivedTabsListEntry> = mutableListOf<ArchivedTabsListEntry>().apply {
        var previousDate: LocalDate? = null
        (0 until tabs.itemCount).forEach { index ->
            // Check if this entry and the previous entry were last active on different days.
            val currentDate = tabs.peek(index)?.lastActiveMs?.toLocalDate()
            if (currentDate != null && currentDate != previousDate) {
                // Add a header that spans the whole LazyVerticalGrid to show the date.
                add(ArchivedTabsListEntry(date = currentDate))
            }
            previousDate = currentDate

            // Add a pointer to the original tab.
            add(ArchivedTabsListEntry(tabsIndex = index))
        }
    }

    /**
     * Returns the [TabData] associated with the given [index]. May be null if the TabData hasn't
     * been loaded or if the entry corresponds to a date header.
     *
     * Implicitly triggers a load for the item if it hasn't been loaded yet.
     */
    fun getTabData(index: Int): TabData? {
        return entries[index].tabsIndex
            ?.takeIf { it >= 0 && it < tabs.itemCount }
            ?.let { tabs[it] }
    }

    fun key(index: Int): Any {
        val entry = entries[index]
        return when {
            entry.date != null -> entry.date

            entry.tabsIndex != null -> {
                entry.tabsIndex
                    .takeIf { it >= 0 && it < tabs.itemCount }
                    ?.let { tabs.peek(it)?.id }
                    ?: PagingPlaceholderKey(entry.tabsIndex)
            }

            else -> throw IllegalStateException()
        }
    }

    fun span(index: Int, numCellsPerRow: Int): GridItemSpan {
        return if (entries[index].date != null) {
            // Dates need to span the whole row.
            GridItemSpan(numCellsPerRow)
        } else {
            // Archived tabs take up one slot.
            GridItemSpan(1)
        }
    }

    /**
     * Forked from Compose's internal version of PagingPlaceholderKey.
     * Satisfies the requirement for a key that is [Parcelable].
     */
    @SuppressLint("BanParcelableUsage")
    data class PagingPlaceholderKey(private val index: Int) : Parcelable {
        override fun writeToParcel(parcel: Parcel, flags: Int) = parcel.writeInt(index)
        override fun describeContents(): Int = 0

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<PagingPlaceholderKey> {
                override fun createFromParcel(parcel: Parcel) =
                    PagingPlaceholderKey(parcel.readInt())

                override fun newArray(size: Int) =
                    arrayOfNulls<PagingPlaceholderKey?>(size)
            }
        }
    }
}
