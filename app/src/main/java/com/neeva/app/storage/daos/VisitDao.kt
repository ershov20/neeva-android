package com.neeva.app.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.neeva.app.storage.entities.Visit
import java.util.Date
import kotlinx.coroutines.flow.Flow

@RewriteQueriesToDropUnusedColumns
@Dao
interface VisitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVisit(visit: Visit)

    @Query(
        """
        DELETE
        FROM Visit
        WHERE visitUID = :id 
    """
    )
    suspend fun deleteVisit(id: Int)

    @Query(
        """
        DELETE
        FROM Visit
        WHERE timestamp >= :from AND timestamp < :to 
    """
    )
    fun deleteVisitsWithinTimeframe(from: Date, to: Date): Int

    @Query(
        """
        SELECT *
        FROM Visit
        WHERE timestamp >= :from AND timestamp < :to
        ORDER BY timestamp DESC
    """
    )
    fun getVisitsWithinTimeframe(from: Date, to: Date): List<Visit>

    @Query(
        """
        SELECT *
        FROM Visit
        WHERE timestamp >= :from AND timestamp < :to
        ORDER BY timestamp DESC
    """
    )
    fun getVisitsWithinTimeframeFlow(from: Date, to: Date): Flow<List<Visit>>
}
