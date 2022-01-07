package com.neeva.app.storage

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.Update
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class EntityType {
    UNKNOWN, ARTICLE, PRODUCT
}

enum class VisitType {
    SEARCH, LINK, TOP_SITES
}

// TODO(dan.alcantara): We should be limiting how large these strings can get -- especially if they
//                      are provided by the website itself.
data class SiteMetadata(
    val imageURL: String? = null,
    val title: String? = null,
    val description: String? = null,
    val entityType: Int = 0,
)

@Entity(indices = [Index(value = ["siteURL"], unique = true)])
data class Site(
    @PrimaryKey(autoGenerate = true) val siteUID: Int = 0,
    val siteURL: String,
    val visitCount: Int = 1,
    val lastVisitTimestamp: Date,
    @Embedded val metadata: SiteMetadata?,
    @Embedded val largestFavicon: Favicon?
)

@Entity
data class Visit(
    @PrimaryKey(autoGenerate = true) val visitUID: Int = 0,
    val visitRootID: Long,
    val visitType: Int,
    val timestamp: Date,

    /** For passing a Visit down the stack and setting the siteUID in repository */
    val visitedSiteUID: Int = 0
)

data class SiteWithVisits(
    @Embedded val site: Site,
    @Relation(
        parentColumn = "siteUID",
        entityColumn = "visitedSiteUID"
    )
    val visits: List<Visit>
)

object DateConverter {
    @TypeConverter
    fun toDate(dateLong: Long?): Date? {
        return dateLong?.let { Date(it) }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
}

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
    fun getQuerySuggestions(query: String, limit: Int = 1): List<Site>

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
            val metadata: SiteMetadata = site.metadata?.let {
                // Don't delete a title if we are trying to update an existing entry.  We may be
                // trying to update an entry from a place where it's not available.
                if (title != null) {
                    it.copy(title = title)
                } else {
                    it
                }
            } ?: SiteMetadata(title = title)

            // Keep the biggest favicon we find.
            val largest = Favicon.bestFavicon(site.largestFavicon, favicon)

            site = site.copy(
                metadata = metadata,
                largestFavicon = largest,
                visitCount = if (visit != null) site.visitCount + 1 else site.visitCount,
                lastVisitTimestamp = visit?.timestamp ?: site.lastVisitTimestamp
            )
            update(site)
        } else {
            site = Site(
                siteURL = url.toString(),
                metadata = SiteMetadata(title = title),
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

class SitesRepository(private val sitesAccessor: SitesWithVisitsAccessor) {
    val allSites: Flow<List<Site>> = sitesAccessor.getAllSites().distinctUntilChanged()
    val allVisits: Flow<List<Visit>> = sitesAccessor.getAllVisits().distinctUntilChanged()

    @WorkerThread
    fun getHistoryAfter(thresholdTime: Date): Flow<List<Site>> =
        sitesAccessor.getVisitsAfter(thresholdTime)
            .map { list ->
                list.mapNotNull {
                    sitesAccessor.get(it.visitedSiteUID)
                }
            }
            .distinctUntilChanged()

    @WorkerThread
    fun getFrequentSitesAfter(thresholdTime: Date, limit: Int): Flow<List<Site>> =
        sitesAccessor.getFrequentSitesAfter(thresholdTime, limit).distinctUntilChanged()

    @WorkerThread
    fun getQuerySuggestions(query: String): List<Site> {
        return sitesAccessor.getQuerySuggestions(query)
    }

    suspend fun insert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) = sitesAccessor.upsert(url, title, favicon, visit)
}
