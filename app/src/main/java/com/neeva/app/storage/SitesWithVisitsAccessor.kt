package com.neeva.app.storage

import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Dao
interface SitesWithVisitsAccessor {
    @Query("SELECT * FROM site")
    fun getAllSites(): Flow<List<Site>>

    @Query("SELECT * FROM visit")
    fun getAllVisits(): Flow<List<Visit>>

    // TODO: Use DataSource.Factory and the Pagination library for this instead after the UI is done
    @Query("SELECT * FROM visit WHERE timestamp > :thresholdTime ORDER BY timestamp DESC")
    fun getVisitsAfter(thresholdTime: Date): Flow<List<Visit>>

    @Query(
        """
        SELECT *
        FROM visit
        WHERE timestamp > :from AND timestamp < :to ORDER BY timestamp DESC
    """
    )
    fun getVisitsWithin(from: Date, to: Date): Flow<List<Visit>>

    @Query(
        """
        SELECT *
        FROM site
        WHERE lastVisitTimestamp > :thresholdTime
        ORDER BY visitCount DESC
        LIMIT :limit
    """
    )
    fun getFrequentSitesAfter(thresholdTime: Date, limit: Int): Flow<List<Site>>

    @Query(
        """
        SELECT * 
        FROM site
        WHERE siteURL LIKE '%'||:query||'%'
        ORDER BY visitCount DESC
        LIMIT :limit
    """
    )
    suspend fun getQuerySuggestions(query: String, limit: Int = 1): List<Site>

    @Transaction
    @Query("SELECT * FROM site")
    fun getSitesWithVisits(): List<SiteWithVisits>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(vararg sites: Site)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(visit: Visit)

    @Update
    suspend fun update(vararg sites: Site)

    @Query("SELECT * FROM site WHERE siteURL LIKE :url")
    suspend fun find(url: String): Site?

    @Query("SELECT * FROM site WHERE siteURL LIKE :url")
    fun getFromUrl(url: String): Flow<Site?>

    @Query("SELECT * FROM site WHERE siteUID LIKE :uid")
    suspend fun get(uid: Int): Site?

    @Transaction
    suspend fun upsert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) {
        var site = find(url.toString())

        if (site != null) {
            val metadata: Site.SiteMetadata = site.metadata?.let {
                // Don't delete a title if we are trying to update an existing entry.  We may be
                // trying to update an entry from a place where it's not available.
                if (title != null) {
                    it.copy(title = title)
                } else {
                    it
                }
            } ?: Site.SiteMetadata(title = title)

            site = site.copy(
                metadata = metadata,
                largestFavicon = favicon ?: site.largestFavicon,
                visitCount = if (visit != null) site.visitCount + 1 else site.visitCount,
                lastVisitTimestamp = visit?.timestamp ?: site.lastVisitTimestamp
            )
            update(site)
        } else {
            site = Site(
                siteURL = url.toString(),
                metadata = Site.SiteMetadata(title = title),
                largestFavicon = favicon,
                lastVisitTimestamp = visit?.timestamp ?: Date()
            )
            add(site)
        }

        visit?.let {
            add(
                it.copy(visitedSiteUID = find(site.siteURL)?.siteUID ?: 0)
            )
        }
    }
}
