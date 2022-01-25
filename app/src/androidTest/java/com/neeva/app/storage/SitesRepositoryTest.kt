package com.neeva.app.storage

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.storage.entities.Visit
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SitesRepositoryTest : HistoryDatabaseBaseTest() {
    private lateinit var sitesRepository: SitesRepository

    override fun setUp() {
        super.setUp()
        sitesRepository = SitesRepository(database.fromSites())
    }

    private suspend fun addSitesIntoDatabase() {
        // Setup: Add entries into the database.  A was visited 3 times, B twice, and C once.
        for (i in 0 until 3) {
            sitesRepository.insert(
                url = Uri.parse("https://www.a.com"),
                title = "Title A",
                favicon = null,
                visit = Visit(
                    visitRootID = 0L,
                    visitType = 0,
                    timestamp = Date(10000L + i),
                )
            )
        }
        for (i in 0 until 2) {
            sitesRepository.insert(
                url = Uri.parse("https://www.b.com"),
                title = "Title B",
                favicon = null,
                visit = Visit(
                    visitRootID = 0L,
                    visitType = 0,
                    timestamp = Date(20000L + i),
                )
            )
        }
        sitesRepository.insert(
            url = Uri.parse("https://www.c.com"),
            title = "Title C",
            favicon = null,
            visit = Visit(
                visitRootID = 0L,
                visitType = 0,
                timestamp = Date(30000L),
            )
        )
    }

    @Test
    fun insert() {
        runBlocking {
            addSitesIntoDatabase()
            val sites = database.fromSites().getFrequentSitesAfter(Date(0L))

            expectThat(sites).hasSize(3)
            expectThat(sites[0].siteURL).isEqualTo("https://www.a.com")
            expectThat(sites[0].metadata?.title).isEqualTo("Title A")
            expectThat(sites[1].siteURL).isEqualTo("https://www.b.com")
            expectThat(sites[1].metadata?.title).isEqualTo("Title B")
            expectThat(sites[2].siteURL).isEqualTo("https://www.c.com")
            expectThat(sites[2].metadata?.title).isEqualTo("Title C")

            val visits = database.fromSites().getVisitsWithinTimeframe(Date(0L), Date(50000L))
            expectThat(visits).hasSize(6)
        }
    }

    @Test
    fun deleteOrphanedSiteEntities() {
        runBlocking {
            addSitesIntoDatabase()
            val sitesBefore = database.fromSites().getAllSites()
            expectThat(sitesBefore).hasSize(3)
            expectThat(sitesBefore.map { it.siteURL })
                .containsExactly("https://www.a.com", "https://www.b.com", "https://www.c.com")

            // Delete some of the recorded Visits.
            // There should still be 3 visits for www.a.com and 1 for www.b.com.
            sitesRepository.deleteHistoryWithinTimeframe(Date(20001L), Date(50000L))
            val sitesAfter = database.fromSites().getAllSites()
            expectThat(sitesAfter).hasSize(2)
            expectThat(sitesAfter.map { it.siteURL })
                .containsExactly("https://www.a.com", "https://www.b.com")
        }
    }
}
