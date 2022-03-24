package com.neeva.app.storage.daos

import androidx.annotation.VisibleForTesting
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neeva.app.storage.entities.Visit
import java.util.Date

@Dao
interface VisitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVisit(visit: Visit)

    @Query(
        """
        DELETE
        FROM Visit
        WHERE timestamp >= :from AND timestamp < :to 
        """
    )
    fun deleteVisitsWithinTimeframe(from: Date, to: Date): Int

    /**
     * This query intentionally ignores whether or not the query is marked for deletion because it
     * is only meant for tests.
     */
    @VisibleForTesting
    @Query(
        """
        SELECT *
        FROM Visit
        WHERE timestamp >= :from AND timestamp < :to
        ORDER BY timestamp DESC
        """
    )
    fun getVisitsWithinTimeframeForTest(from: Date, to: Date): List<Visit>

    /** Marks or unmarks an entry for deletion during the next purge. */
    @Query(
        """
        UPDATE Visit
        SET isMarkedForDeletion = :isMarkedForDeletion
        WHERE visitUID = :id
        """
    )
    fun setMarkedForDeletion(id: Int, isMarkedForDeletion: Boolean)

    /*** Remove all entries from the table that are marked for deletion. */
    @Query(
        """
        DELETE
        FROM Visit
        WHERE isMarkedForDeletion
        """
    )
    fun purgeVisitsMarkedForDeletion(): Int
}
