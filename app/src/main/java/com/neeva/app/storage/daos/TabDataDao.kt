package com.neeva.app.storage.daos

import androidx.annotation.VisibleForTesting
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neeva.app.storage.entities.TabData

@Dao
abstract class TabDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun add(tabData: TabData)

    @Query(
        """
        SELECT *
        FROM TabData
        WHERE id = :id
        """
    )
    abstract fun get(id: String): TabData?

    @VisibleForTesting
    @Query("SELECT * FROM TabData")
    abstract fun getAll(): List<TabData>

    @Query(
        """
        SELECT      *
        FROM        TabData
        WHERE       isArchived = 1
        ORDER BY    lastActiveMs DESC
        """
    )
    abstract fun getAllArchived(): List<TabData>

    @Query(
        """
        SELECT      *
        FROM        TabData
        WHERE       isArchived = 1
        ORDER BY    lastActiveMs DESC
        """
    )
    abstract fun getAllArchivedPaged(): PagingSource<Int, TabData>

    @Query("DELETE FROM TabData WHERE id = :id")
    abstract fun delete(id: String)

    @Query(
        """
        DELETE
        FROM    TabData
        WHERE   isArchived = 1
        """
    )
    abstract fun deleteAllArchived()
}
