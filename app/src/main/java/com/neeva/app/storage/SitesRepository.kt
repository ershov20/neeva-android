package com.neeva.app.storage

import android.net.Uri
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SitesRepository(private val sitesAccessor: SitesWithVisitsAccessor) {
    val allSites: Flow<List<Site>> = sitesAccessor.getAllSites().distinctUntilChanged()
    val allVisits: Flow<List<Visit>> = sitesAccessor.getAllVisits().distinctUntilChanged()

    fun getHistoryAfter(thresholdTime: Date): Flow<List<Site>> =
        sitesAccessor.getVisitsAfter(thresholdTime)
            .map { list ->
                list.mapNotNull {
                    sitesAccessor.get(it.visitedSiteUID)
                }
            }
            .distinctUntilChanged()

    fun getFrequentSitesAfter(thresholdTime: Date, limit: Int): Flow<List<Site>> =
        sitesAccessor.getFrequentSitesAfter(thresholdTime, limit).distinctUntilChanged()

    suspend fun getQuerySuggestions(query: String): List<Site> =
        sitesAccessor.getQuerySuggestions(query)

    suspend fun find(url: Uri): Site? = sitesAccessor.find(url.toString())

    suspend fun insert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) = sitesAccessor.upsert(url, title, favicon, visit)
}
