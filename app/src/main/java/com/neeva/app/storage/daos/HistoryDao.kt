// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage.daos

import android.net.Uri
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import com.neeva.app.storage.entities.Favicon
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit
import java.util.Date
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@RewriteQueriesToDropUnusedColumns
@Dao
interface HistoryDao : SiteDao, VisitDao {

    @Query(
        """
        SELECT *
        FROM site INNER JOIN visit ON site.siteUID = visit.visitedSiteUID
        WHERE visit.timestamp >= :thresholdTime
              AND (Site.siteURL LIKE '%'||:query||'%' OR Site.title LIKE '%'||:query||'%')
              AND NOT visit.isMarkedForDeletion
        ORDER BY visit.timestamp DESC
        """
    )
    fun getPagedSitesVisitedAfter(
        thresholdTime: Date,
        query: String = ""
    ): PagingSource<Int, SitePlusVisit>

    /** Deletes any entries in the [Site] table that have no corresponding [Visit] information. */
    @Query(
        """
            DELETE
            FROM site
            WHERE site.siteUID IN (
                SELECT site.siteUID
                FROM site LEFT JOIN visit ON site.siteUID = visit.visitedSiteUID
                WHERE visit.visitUID IS NULL
                GROUP BY site.siteUID
            )
        """
    )
    fun deleteOrphanedSiteEntities(): Int

    @Query(
        """
        SELECT *
        FROM Site INNER JOIN Visit ON Site.siteUID = Visit.visitedSiteUID
        WHERE visit.timestamp >= :thresholdTime
              AND NOT Visit.isMarkedForDeletion
        GROUP BY site.siteUID
        ORDER BY COUNT(*) DESC
        LIMIT :limit
        """
    )
    fun getFrequentSitesAfter(thresholdTime: Date, limit: Int = 10): List<Site>

    @Query(
        """
        SELECT *
        FROM Site INNER JOIN Visit ON Site.siteUID = Visit.visitedSiteUID
        WHERE visit.timestamp >= :thresholdTime
              AND NOT Visit.isMarkedForDeletion
        GROUP BY site.siteUID
        ORDER BY COUNT(*) DESC
        LIMIT :limit
        """
    )
    fun getFrequentSitesAfterFlow(thresholdTime: Date, limit: Int = 10): Flow<List<Site>>

    @Query(
        """
        SELECT *
        FROM Site INNER JOIN Visit ON Site.siteUID = Visit.visitedSiteUID
        WHERE (Site.siteURL LIKE '%'||:query||'%' OR Site.title LIKE '%'||:query||'%') 
              AND NOT Visit.isMarkedForDeletion
        GROUP BY Site.siteUID
        ORDER BY COUNT(*) DESC
        LIMIT :limit
    """
    )
    /**
     * Get the most frequently visited entries from history that match the given [query] in either
     * the title or the site URL.
     */
    suspend fun getFrequentHistorySuggestions(query: String, limit: Int = 10): List<Site>

    @Query(
        """
        SELECT *
        FROM Site INNER JOIN Visit ON Site.siteUID = Visit.visitedSiteUID
        WHERE Site.siteURL LIKE '%'||:query||'%'
              AND NOT Visit.isMarkedForDeletion
        GROUP BY Site.siteUID
        ORDER BY timestamp DESC
        LIMIT :limit
    """
    )
    /**
     * Get the most recent URLs from history that match the given query.
     *
     * To get the most recently performed searches, pass in [neevaConstants.appSearchURL] as the
     * query.
     */
    fun getRecentHistorySuggestionsFlow(query: String, limit: Int = 3): Flow<List<Site>>

    @Query(
        """
        SELECT *
        FROM Site INNER JOIN Visit ON Site.siteUID = Visit.visitedSiteUID
        WHERE Site.siteURL LIKE '%'||:query||'%' 
              AND NOT Visit.isMarkedForDeletion
        GROUP BY Site.siteUID
        ORDER BY timestamp DESC
        LIMIT :limit
    """
    )
    fun getRecentHistorySuggestions(query: String, limit: Int = 3): List<Site>

    @Transaction
    suspend fun upsert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) {
        var site = getSiteByUrl(url.toString())

        if (site != null) {
            // Don't replace the title if we are trying to update an existing entry because we may
            // be trying to update an entry from a place where it's not available.
            site = site.copy(
                title = title.takeUnless { title.isNullOrBlank() } ?: site.title,
                largestFavicon = favicon ?: site.largestFavicon
            )

            updateSite(site)
        } else {
            site = Site(
                siteURL = url.toString(),
                title = title,
                largestFavicon = favicon
            )

            addSite(site)
        }

        visit?.let {
            addVisit(
                it.copy(visitedSiteUID = getSiteByUrl(site.siteURL)?.siteUID ?: 0)
            )
        }
    }

    @Transaction
    suspend fun deleteHistoryWithinTimeframe(startTime: Date, endTime: Date) {
        // Delete any visits that happened within the given timeframe.
        val numDeletedVisits = deleteVisitsWithinTimeframe(startTime, endTime)
        Timber.d("Deleted $numDeletedVisits entries from history")

        // Delete Site entities that don't have any remaining Visit entities.
        val numDeletedSites = deleteOrphanedSiteEntities()
        Timber.d("Deleted $numDeletedSites sites from history")
    }
}
