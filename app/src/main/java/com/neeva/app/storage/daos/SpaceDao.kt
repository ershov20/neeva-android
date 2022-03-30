package com.neeva.app.storage.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao : SpaceItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSpace(vararg space: Space)

    @Query("DELETE FROM space")
    suspend fun deleteAllSpaces()

    @Update
    suspend fun updateSpace(vararg space: Space)

    @Query("SELECT * FROM space WHERE id = :id")
    suspend fun getSpaceById(id: String): Space?

    @Query("SELECT id FROM space")
    suspend fun allSpaceIds(): List<String>

    @Query("SELECT * FROM space")
    fun allSpacesFlow(): Flow<List<Space>>

    @Query("SELECT * FROM space")
    fun allSpaces(): List<Space>

    @Transaction
    suspend fun upsert(space: Space) {
        when (getSpaceById(space.id)) {
            null -> addSpace(space)
            else -> updateSpace(space)
        }
    }

    @Delete
    suspend fun deleteSpace(vararg space: Space)

    @Transaction
    suspend fun deleteSpaceById(id: String) {
        getSpaceById(id)?.let { deleteSpace(it) }
    }

    /** Deletes any entries in the [SpaceItem] table that have no corresponding [Space] information. */
    @Query(
        """
            DELETE
            FROM spaceItem
            WHERE spaceItem.id IN (
                SELECT spaceItem.id
                FROM spaceItem LEFT JOIN space ON spaceItem.spaceID = space.id
                WHERE space.id IS NULL
                GROUP BY spaceItem.id
            )
        """
    )
    fun deleteOrphanedSpaceItems(): Int
}
