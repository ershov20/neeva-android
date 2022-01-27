package com.neeva.app.storage.daos

import android.net.Uri
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

    suspend fun getSiteByUrl(uri: Uri) = getSiteByUrl(uri.toString())

    @Query("SELECT * FROM site WHERE siteUID = :uid")
    suspend fun getSiteByUid(uid: Int): Site?

    @Query("SELECT * FROM site")
    suspend fun getAllSites(): List<Site>

    @Query("SELECT DISTINCT(faviconURL) FROM site")
    suspend fun getAllFavicons(): List<String>
}
