package com.neeva.app.cardgrid.tabs

import android.content.Context
import com.neeva.app.browsing.AgeGroup
import com.neeva.app.browsing.AgeGroupCalculator
import com.neeva.app.browsing.ArchiveAfterOption
import com.neeva.app.browsing.TabInfo

data class TabGridSection<T>(
    val header: String,
    val items: MutableList<T> = mutableListOf()
)

fun computeTabGridSections(
    context: Context,
    tabs: List<TabInfo>,
    archiveAfterOption: ArchiveAfterOption,
    now: Long = System.currentTimeMillis()
): List<TabGridSection<TabInfo>> {
    // It'd be more correct to make the time be a State, too, but it'd be expensive because we would
    // be re-filtering the list every time the time changed.
    val timeBuckets = AgeGroupCalculator(now)

    // Bucket the items by how old they are.
    val mappedSections = mutableMapOf<AgeGroup, TabGridSection<TabInfo>>()
    tabs.forEach { tabInfo ->
        if (tabInfo.isArchived(archiveAfterOption, now)) return@forEach

        val ageBucket = tabInfo.getAgeGroup(timeBuckets)
        mappedSections
            .getOrPut(ageBucket) { TabGridSection(context.getString(ageBucket.resourceId)) }
            .items
            .add(tabInfo)
    }

    return mappedSections
        .mapValues { entry ->
            // Sort the items in each bucket by how old they are.
            entry.value.copy(
                items = entry.value.items
                    .sortedByDescending { it.data.lastActiveMs }
                    .toMutableList()
            )
        }
        .toList()
        .sortedBy { it.first.ordinal }
        .map { it.second }
}
