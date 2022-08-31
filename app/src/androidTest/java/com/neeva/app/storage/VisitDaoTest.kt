package com.neeva.app.storage

import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.storage.daos.VisitDao
import com.neeva.app.storage.entities.Visit
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.doesNotContain
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class VisitDaoTest : HistoryDatabaseBaseTest() {
    private lateinit var visitDao: VisitDao

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    override fun setUp() {
        super.setUp()
        visitDao = database.historyDao()
    }

    @Test
    fun getVisitsWithinTimeframe_returnsThingsInsideOfWindow() = runTest {
        // Add a couple of visits to the database.
        visitDao.addVisit(Visit(timestamp = Date(1000L)))
        visitDao.addVisit(Visit(timestamp = Date(2000L)))
        visitDao.addVisit(Visit(timestamp = Date(3000L)))
        visitDao.addVisit(Visit(timestamp = Date(4000L)))
        visitDao.addVisit(Visit(timestamp = Date(5000L)))

        // The time ranges include the start time but not the end time.  Results are returned in
        // backwards order.
        val results = visitDao.getVisitsWithinTimeframeForTest(Date(2000L), Date(4000L))
        expectThat(results).hasSize(2)
        expectThat(results[0].timestamp).isEqualTo(Date(3000L))
        expectThat(results[1].timestamp).isEqualTo(Date(2000L))
    }

    @Test
    fun deleteVisitsWithinTimeframe() = runTest {
        // Add a couple of visits to the database.
        visitDao.addVisit(Visit(timestamp = Date(1000L)))
        visitDao.addVisit(Visit(timestamp = Date(2000L)))
        visitDao.addVisit(Visit(timestamp = Date(3000L)))
        visitDao.addVisit(Visit(timestamp = Date(4000L)))
        visitDao.addVisit(Visit(timestamp = Date(5000L)))

        val numDeleted = visitDao.deleteVisitsWithinTimeframe(Date(3000L), Date(5000L))
        expectThat(numDeleted).isEqualTo(2)

        // Get all possible visits and confirm that the items were deleted.
        val visits = visitDao.getVisitsWithinTimeframeForTest(Date(0L), Date(10000L))
        expectThat(visits).hasSize(3)
        expectThat(visits[0].timestamp).isEqualTo(Date(5000L))
        expectThat(visits[1].timestamp).isEqualTo(Date(2000L))
        expectThat(visits[2].timestamp).isEqualTo(Date(1000L))
    }

    @Test
    fun setMarkedForDeletion() = runTest {
        // Add a couple of visits to the database.
        visitDao.addVisit(Visit(timestamp = Date(1000L)))
        visitDao.addVisit(Visit(timestamp = Date(2000L)))
        visitDao.addVisit(Visit(timestamp = Date(3000L)))
        visitDao.addVisit(Visit(timestamp = Date(4000L)))
        visitDao.addVisit(Visit(timestamp = Date(5000L)))

        // Get all possible visits and pick one to delete.
        val visitsBefore = visitDao.getVisitsWithinTimeframeForTest(Date(0L), Date(10000L))
        expectThat(visitsBefore).hasSize(5)

        // Mark one of them for deletion.
        val visitToDelete = visitsBefore[3]
        visitDao.setMarkedForDeletion(visitToDelete.visitUID, true)

        // Confirm that the visit is still there.
        val visitsWithoutPurge = visitDao.getVisitsWithinTimeframeForTest(Date(0L), Date(10000L))
        expectThat(visitsWithoutPurge).hasSize(5)
        expectThat(visitsWithoutPurge.map { it.visitUID }).contains(visitToDelete.visitUID)
        expectThat(visitsWithoutPurge.map { it.timestamp }).contains(visitToDelete.timestamp)

        // Purge the table and confirm the entry is now gone.
        visitDao.purgeVisitsMarkedForDeletion()
        val visitsAfterPurge = visitDao.getVisitsWithinTimeframeForTest(Date(0L), Date(10000L))
        expectThat(visitsAfterPurge).hasSize(4)
        expectThat(visitsAfterPurge.map { it.visitUID }).doesNotContain(visitToDelete.visitUID)
        expectThat(visitsAfterPurge.map { it.timestamp }).doesNotContain(visitToDelete.timestamp)
    }
}
