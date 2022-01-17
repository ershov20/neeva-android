package com.neeva.app.storage

import android.net.Uri
import androidx.paging.PagingSource
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class SitesRepository(private val sitesAccessor: SitesWithVisitsAccessor) {
    fun getHistoryBetween(startTime: Date, endTime: Date): PagingSource<Int, Site> =
        sitesAccessor.getPagedSitesVisitedBetween(startTime, endTime)

    fun getHistoryAfter(thresholdTime: Date): PagingSource<Int, Site> =
        sitesAccessor.getPagedSitesVisitedAfter(thresholdTime)

    fun getFrequentSitesAfter(thresholdTime: Date, limit: Int): Flow<List<Site>> =
        sitesAccessor.getFrequentSitesAfter(thresholdTime, limit).distinctUntilChanged()

    suspend fun getQuerySuggestions(query: String, limit: Int): List<Site> =
        sitesAccessor.getQuerySuggestions(query, limit)

    suspend fun find(url: Uri): Site? = sitesAccessor.findSite(url.toString())

    suspend fun insert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) = sitesAccessor.upsert(url, title, favicon, visit)
}
