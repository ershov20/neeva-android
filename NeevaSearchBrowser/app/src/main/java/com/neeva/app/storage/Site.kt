package com.neeva.app.storage

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import androidx.room.*
import com.neeva.app.R
import com.neeva.app.appSearchURL
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.web.baseDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

enum class EntityType {
    UNKNOWN, ARTICLE, PRODUCT
}

enum class VisitType {
    SEARCH, LINK, TOP_SITES
}

data class SiteMetadata(
    val imageURL: String? = null,
    val title: String? = null,
    val description: String? = null,
    val entityType: Int = 0,
)

@Entity(indices = [Index(value = ["siteURL"], unique = true)])
data class Site(
    @PrimaryKey (autoGenerate = true) val siteUID: Int = 0,
    val siteURL: String,
    val visitCount: Int = 1,
    val lastVisitTimestamp: Date,
    @Embedded val metadata: SiteMetadata?,
    @Embedded val largestFavicon: Favicon?
)

@Entity
data class Visit(
    @PrimaryKey (autoGenerate = true) val visitUID: Int = 0,
    val visitRootID: Long,
    val visitType: Int,
    val timestamp: Date,
    val visitedSiteUID: Int = 0, // For passing a Visit down the stack and setting the siteUID in repository
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

    @Query("SELECT * FROM visit WHERE timestamp > :from AND timestamp < :to ORDER BY timestamp DESC")
    fun getVisitsWithin(from: Date, to: Date): Flow<List<Visit>>

    @Query("SELECT * FROM site WHERE lastVisitTimestamp > :thresholdTime ORDER BY visitCount DESC LIMIT :limit")
    fun getFrequentSitesAfter(thresholdTime: Date, limit: Int): Flow<List<Site>>

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
}

class SitesRepository(private val sitesAccessor: SitesWithVisitsAccessor) {
    val allSites: Flow<List<Site>> = sitesAccessor.getAllSites().distinctUntilChanged()
    val allVisits: Flow<List<Visit>> = sitesAccessor.getAllVisits().distinctUntilChanged()

    @WorkerThread
    fun getHistoryAfter(thresholdTime: Date): Flow<List<Site>> =
        sitesAccessor.getVisitsAfter(thresholdTime).map { list ->
            list.mapNotNull { sitesAccessor.get(it.visitedSiteUID) }
        }.distinctUntilChanged()

    @WorkerThread
    fun getHistoryWithin(from: Date, to:Date): Flow<List<Pair<Site, Visit>>> =
        sitesAccessor.getVisitsWithin(from = from, to = to).map { list ->
            list.mapNotNull { Pair(sitesAccessor.get(it.visitedSiteUID)!!, it) }
        }.distinctUntilChanged()

    @WorkerThread
    fun getFrequentSitesAfter(thresholdTime: Date, limit: Int): Flow<List<Site>> =
        sitesAccessor.getFrequentSitesAfter(thresholdTime, limit).distinctUntilChanged()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    @Transaction
    suspend fun insert(url: Uri, title: String? = null, favicon: Favicon? = null, visit: Visit? = null) {
        var site = sitesAccessor.find(url.toString())
        if (site !=  null) {
            val metadata = site.metadata?.copy(title = title) ?: SiteMetadata(title = title)
            val largest = if (site.largestFavicon != null)
                site.largestFavicon!! larger favicon else favicon
            sitesAccessor.update(site.copy(metadata = metadata, largestFavicon = largest,
                visitCount = if (visit != null) site.visitCount + 1 else site.visitCount,
                lastVisitTimestamp = visit?.timestamp ?: site.lastVisitTimestamp))
        } else {
            site = Site(
                siteURL = url.toString(),
                metadata = SiteMetadata(title = title),
                largestFavicon = favicon,
                lastVisitTimestamp = visit?.timestamp ?: Date())
            sitesAccessor.add(site)
        }

        visit?.let {
            sitesAccessor.add(it.copy(
                visitedSiteUID = sitesAccessor.find(site.siteURL)?.siteUID ?: 0))
        }
    }
}