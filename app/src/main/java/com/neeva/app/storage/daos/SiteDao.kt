package com.neeva.app.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.neeva.app.storage.entities.Site

@RewriteQueriesToDropUnusedColumns
@Dao
interface SiteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSite(vararg sites: Site)

    @Update
    suspend fun updateSite(vararg sites: Site)

    @Query("SELECT * FROM site WHERE siteURL = :url")
    suspend fun getSiteByUrl(url: String): Site?

    @Query("SELECT * FROM site WHERE siteUID = :uid")
    suspend fun getSiteByUid(uid: Int): Site?

    @Query("SELECT * FROM site")
    suspend fun getAllSites(): List<Site>
}
