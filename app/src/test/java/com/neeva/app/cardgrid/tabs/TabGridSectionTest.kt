package com.neeva.app.cardgrid.tabs

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.R
import com.neeva.app.browsing.ArchiveAfterOption
import com.neeva.app.browsing.TabInfo
import com.neeva.app.ui.toEpochMilli
import com.neeva.app.ui.toLocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class TabGridSectionTest : BaseTest() {
    private lateinit var context: Context

    private lateinit var todayString: String
    private lateinit var yesterdayString: String
    private lateinit var lastWeekString: String
    private lateinit var lastMonthString: String
    private lateinit var olderString: String

    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        todayString = context.getString(R.string.archived_tabs_today)
        yesterdayString = context.getString(R.string.archived_tabs_yesterday)
        lastWeekString = context.getString(R.string.archived_tabs_last_seven_days)
        lastMonthString = context.getString(R.string.archived_tabs_last_thirty_days)
        olderString = context.getString(R.string.archived_tabs_older)
    }

    private fun createTabs(selectedTabIndex: Int, now: LocalDateTime): List<TabInfo> {
        return mutableListOf<TabInfo>().apply {
            (0 until 35).forEach {
                add(
                    TabInfo(
                        id = "tab $it",
                        url = Uri.parse("https://www.neeva.com/$it"),
                        title = "title $it",
                        isSelected = it == selectedTabIndex,
                        data = TabInfo.PersistedData(
                            lastActiveMs = now.minusDays(it.toLong()).toEpochMilli()
                        )
                    )
                )
            }
        }
    }

    @Test
    fun computeTabGridSections_archiveAfter7Days() {
        val now = LocalDateTime.of(
            TimeUnit.DAYS.toMillis(1000).toLocalDate(),
            LocalTime.NOON
        )

        val selectedTabIndex = 5
        val tabs = createTabs(selectedTabIndex, now)

        val sections = computeTabGridSections(
            context = context,
            tabs = tabs,
            archiveAfterOption = ArchiveAfterOption.AFTER_7_DAYS,
            now = now.toEpochMilli()
        )

        // The today section should have today's tabs plus the active tab.
        expectThat(sections).hasSize(3)
        expectThat(sections[0]).isEqualTo(
            TabGridSection(
                header = todayString,
                items = mutableListOf(tabs[0], tabs[5])
            )
        )
        expectThat(sections[1]).isEqualTo(
            TabGridSection(
                header = yesterdayString,
                items = mutableListOf(tabs[1])
            )
        )
        expectThat(sections[2]).isEqualTo(
            TabGridSection(
                header = lastWeekString,
                items = mutableListOf(tabs[2], tabs[3], tabs[4], tabs[6], tabs[7])
            )
        )
    }

    @Test
    fun computeTabGridSections_archiveAfter30Days() {
        val now = LocalDateTime.of(
            TimeUnit.DAYS.toMillis(1000).toLocalDate(),
            LocalTime.NOON
        )

        val selectedTabIndex = 5
        val tabs = createTabs(selectedTabIndex, now)

        val sections = computeTabGridSections(
            context = context,
            tabs = tabs,
            archiveAfterOption = ArchiveAfterOption.AFTER_30_DAYS,
            now = now.toEpochMilli()
        )

        // The today section should have today's tabs plus the active tab.
        expectThat(sections).hasSize(4)
        expectThat(sections[0]).isEqualTo(
            TabGridSection(
                header = todayString,
                items = mutableListOf(tabs[0], tabs[5])
            )
        )
        expectThat(sections[1]).isEqualTo(
            TabGridSection(
                header = yesterdayString,
                items = mutableListOf(tabs[1])
            )
        )
        expectThat(sections[2]).isEqualTo(
            TabGridSection(
                header = lastWeekString,
                items = mutableListOf(tabs[2], tabs[3], tabs[4], tabs[6], tabs[7])
            )
        )
        expectThat(sections[3]).isEqualTo(
            TabGridSection(
                header = lastMonthString,
                items = tabs.subList(8, 31).toMutableList()
            )
        )
    }

    @Test
    fun computeTabGridSections_archiveNever() {
        val now = LocalDateTime.of(
            TimeUnit.DAYS.toMillis(1000).toLocalDate(),
            LocalTime.NOON
        )

        val selectedTabIndex = 5
        val tabs = createTabs(selectedTabIndex, now)

        val sections = computeTabGridSections(
            context = context,
            tabs = tabs,
            archiveAfterOption = ArchiveAfterOption.NEVER,
            now = now.toEpochMilli()
        )

        // The today section should have today's tabs plus the active tab.
        expectThat(sections).hasSize(5)
        expectThat(sections[0]).isEqualTo(
            TabGridSection(
                header = todayString,
                items = mutableListOf(tabs[0], tabs[5])
            )
        )
        expectThat(sections[1]).isEqualTo(
            TabGridSection(
                header = yesterdayString,
                items = mutableListOf(tabs[1])
            )
        )
        expectThat(sections[2]).isEqualTo(
            TabGridSection(
                header = lastWeekString,
                items = mutableListOf(tabs[2], tabs[3], tabs[4], tabs[6], tabs[7])
            )
        )
        expectThat(sections[3]).isEqualTo(
            TabGridSection(
                header = lastMonthString,
                items = tabs.subList(8, 31).toMutableList()
            )
        )
        expectThat(sections[4]).isEqualTo(
            TabGridSection(
                header = olderString,
                items = tabs.subList(31, tabs.size).toMutableList()
            )
        )
    }
}
