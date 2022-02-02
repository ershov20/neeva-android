package com.neeva.app.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.storage.daos.VisitDao
import com.neeva.app.storage.entities.Visit
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.doesNotContain
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VisitDaoTest : HistoryDatabaseBaseTest() {
    private lateinit var visitDao: VisitDao

    override fun setUp() {
        super.setUp()
        visitDao = database.dao()
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
        val results = visitDao.getVisitsWithinTimeframe(Date(2000L), Date(4000L))
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
        val visits = visitDao.getVisitsWithinTimeframe(Date(0L), Date(10000L))
        expectThat(visits).hasSize(3)
        expectThat(visits[0].timestamp).isEqualTo(Date(5000L))
        expectThat(visits[1].timestamp).isEqualTo(Date(2000L))
        expectThat(visits[2].timestamp).isEqualTo(Date(1000L))
    }

    @Test
    fun deleteVisit() = runTest {
        // Add a couple of visits to the database.
        visitDao.addVisit(Visit(timestamp = Date(1000L)))
        visitDao.addVisit(Visit(timestamp = Date(2000L)))
        visitDao.addVisit(Visit(timestamp = Date(3000L)))
        visitDao.addVisit(Visit(timestamp = Date(4000L)))
        visitDao.addVisit(Visit(timestamp = Date(5000L)))

        // Get all possible visits and pick one to delete.
        val visitsBefore = visitDao.getVisitsWithinTimeframe(Date(0L), Date(10000L))
        expectThat(visitsBefore).hasSize(5)

        // Delete one of them.
        val visitToDelete = visitsBefore[3]
        visitDao.deleteVisit(visitToDelete.visitUID)

        // Confirm that the visit is now gone.
        val visitsAfter = visitDao.getVisitsWithinTimeframe(Date(0L), Date(10000L))
        expectThat(visitsAfter).hasSize(4)
        expectThat(visitsAfter.map { it.visitUID }).doesNotContain(visitToDelete.visitUID)
        expectThat(visitsAfter.map { it.timestamp }).doesNotContain(visitToDelete.timestamp)
    }
}
