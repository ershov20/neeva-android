package com.neeva.app.storage

import android.net.Uri
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.storage.daos.HistoryDao
import com.neeva.app.storage.entities.Visit
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@HiltAndroidTest
class HistoryDaoTest : HistoryDatabaseBaseTest() {
    private lateinit var sitesRepository: HistoryDao

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    override fun setUp() {
        super.setUp()
        sitesRepository = database.dao()
    }

    private suspend fun addSitesIntoDatabase() {
        // Setup: Add entries into the database.  A was visited 3 times, B twice, and C once.
        for (i in 0 until 3) {
            sitesRepository.upsert(
                url = Uri.parse("https://www.a.com"),
                title = "Title A",
                favicon = null,
                visit = Visit(timestamp = Date(10000L + i))
            )
        }
        for (i in 0 until 2) {
            sitesRepository.upsert(
                url = Uri.parse("https://www.b.com"),
                title = "Title B",
                favicon = null,
                visit = Visit(timestamp = Date(20000L + i))
            )
        }
        sitesRepository.upsert(
            url = Uri.parse("https://www.c.com"),
            title = "Title C",
            favicon = null,
            visit = Visit(timestamp = Date(30000L))
        )
    }

    @Test
    fun insert() {
        runBlocking {
            addSitesIntoDatabase()
            val sites = database.dao().getFrequentSitesAfter(Date(0L))

            expectThat(sites).hasSize(3)
            expectThat(sites[0].siteURL).isEqualTo("https://www.a.com")
            expectThat(sites[0].title).isEqualTo("Title A")
            expectThat(sites[1].siteURL).isEqualTo("https://www.b.com")
            expectThat(sites[1].title).isEqualTo("Title B")
            expectThat(sites[2].siteURL).isEqualTo("https://www.c.com")
            expectThat(sites[2].title).isEqualTo("Title C")

            val visits = database.dao().getVisitsWithinTimeframeForTest(Date(0L), Date(50000L))
            expectThat(visits).hasSize(6)
        }
    }

    @Test
    fun deleteOrphanedSiteEntities() {
        runBlocking {
            addSitesIntoDatabase()
            val sitesBefore = database.dao().getAllSites()
            expectThat(sitesBefore).hasSize(3)
            expectThat(sitesBefore.map { it.siteURL })
                .containsExactly("https://www.a.com", "https://www.b.com", "https://www.c.com")

            // Delete some of the recorded Visits.
            // There should still be 3 visits for www.a.com and 1 for www.b.com.
            sitesRepository.deleteHistoryWithinTimeframe(Date(20001L), Date(50000L))
            val sitesAfter = database.dao().getAllSites()
            expectThat(sitesAfter).hasSize(2)
            expectThat(sitesAfter.map { it.siteURL })
                .containsExactly("https://www.a.com", "https://www.b.com")
        }
    }

    @Test
    fun getFrequentHistorySuggestions_byTitle() {
        runBlocking {
            addSitesIntoDatabase()
            val sites = database.dao().getFrequentHistorySuggestions("Title")
            expectThat(sites.map { it.siteURL })
                .containsExactly("https://www.a.com", "https://www.b.com", "https://www.c.com")

            // Delete some of the recorded Visits.
            // There should still be 3 visits for www.a.com and 1 for www.b.com.
            sitesRepository.deleteHistoryWithinTimeframe(Date(20001L), Date(50000L))
            val sitesAfter = database.dao().getFrequentHistorySuggestions("Title")
            expectThat(sitesAfter).hasSize(2)
            expectThat(sitesAfter.map { it.siteURL })
                .containsExactly("https://www.a.com", "https://www.b.com")
        }
    }

    @Test
    fun getFrequentHistorySuggestions_byUrl() {
        runBlocking {
            addSitesIntoDatabase()
            expectThat(database.dao().getFrequentHistorySuggestions("com").map { it.siteURL })
                .containsExactly("https://www.a.com", "https://www.b.com", "https://www.c.com")

            expectThat(database.dao().getFrequentHistorySuggestions("c.com").map { it.siteURL })
                .containsExactly("https://www.c.com")
        }
    }

    @Test
    fun getRecentHistorySuggestions() {
        runBlocking {
            addSitesIntoDatabase()
            expectThat(database.dao().getRecentHistorySuggestions("com").map { it.siteURL })
                .containsExactly("https://www.c.com", "https://www.b.com", "https://www.a.com")

            expectThat(database.dao().getRecentHistorySuggestions("c.com").map { it.siteURL })
                .containsExactly("https://www.c.com")
        }
    }
}
