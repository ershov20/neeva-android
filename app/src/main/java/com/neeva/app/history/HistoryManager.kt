package com.neeva.app.history

import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.daos.SitePlusVisit
import com.neeva.app.storage.entities.Favicon
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.zeroQuery.toSearchSuggest
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Provides access to the user's navigation history. */
class HistoryManager(
    historyDatabase: HistoryDatabase,
    private val domainProvider: DomainProvider,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaConstants: NeevaConstants
) {
    companion object {
        private const val MAX_FREQUENT_SITES = 40
        private const val PAGE_SIZE = 30

        private val HISTORY_WINDOW = TimeUnit.DAYS.toMillis(7)
        private val HISTORY_START_DATE = Date(System.currentTimeMillis() - HISTORY_WINDOW)

        private val TAG = HistoryManager::class.simpleName
    }

    private val dao = historyDatabase.dao()

    fun getHistoryAfter(startTime: Date): Flow<PagingData<SitePlusVisit>> {
        return Pager(PagingConfig(pageSize = PAGE_SIZE)) {
            dao.getPagedSitesVisitedAfter(startTime)
        }.flow
    }

    private val frequentSites: Flow<List<Site>> =
        dao.getFrequentSitesAfterFlow(HISTORY_START_DATE, MAX_FREQUENT_SITES)

    private val _historySuggestions = MutableStateFlow<List<NavSuggestion>>(emptyList())
    val historySuggestions: StateFlow<List<NavSuggestion>> = _historySuggestions

    /** Provides the top 3 suggestions based on how often a user visited a site. */
    val suggestedQueries: Flow<List<QueryRowSuggestion>> = dao
        .getQuerySuggestionsFlow(query = neevaConstants.appSearchURL)
        .map { siteList ->
            siteList.mapNotNull { it.toSearchSuggest(neevaConstants) }.take(3)
        }

    /** Provides non-Neeva sites from history as suggestions. */
    val suggestedSites: Flow<List<Site>> =
        frequentSites.map { sites ->
            // Assume that anything pointing at neeva.com should not be recommended to the user.
            // This includes search suggestions and Spaces, e.g.
            sites.filterNot {
                val registeredDomain = domainProvider.getRegisteredDomain(Uri.parse(it.siteURL))
                registeredDomain == neevaConstants.appHost
            }
        }

    /** Updates the query that is being used to fetch history suggestions. */
    suspend fun updateSuggestionQuery(currentInput: String?) {
        val siteSuggestions = if (currentInput != null) {
            dao.getQuerySuggestions(currentInput, limit = 10)
        } else {
            emptyList()
        }

        _historySuggestions.value = siteSuggestions.map { it.toNavSuggestion(domainProvider) }
    }

    /** Returns the favicon that corresponds to an exact visit in the user's history. */
    suspend fun getFaviconFromHistory(uri: Uri): Favicon? {
        val siteFavicon = dao.getSiteByUrl(uri)?.largestFavicon
        if (siteFavicon != null) return siteFavicon
        return null
    }

    /** Inserts or updates an item into the history. */
    suspend fun upsert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) {
        withContext(dispatchers.io) {
            dao.upsert(url, title, favicon, visit)
        }
    }

    /** Marks a visit for deletion when the database is next pruned. */
    fun markVisitForDeletion(visitUID: Int, isMarkedForDeletion: Boolean) {
        coroutineScope.launch(dispatchers.io) {
            dao.setMarkedForDeletion(visitUID, isMarkedForDeletion = isMarkedForDeletion)
        }
    }

    /** Cleans out entries from the database that are no longer necessary. */
    fun pruneDatabase() {
        val numVisitsPurged = dao.purgeVisitsMarkedForDeletion()
        Log.d(TAG, "Purged $numVisitsPurged visits from the database")

        val numSitesPurged = dao.deleteOrphanedSiteEntities()
        Log.d(TAG, "Purged $numSitesPurged orphaned sites from the database")
    }

    fun clearHistory(fromMillis: Long) {
        coroutineScope.launch(dispatchers.io) {
            dao.deleteHistoryWithinTimeframe(
                Date(fromMillis),
                Date()
            )
        }
    }

    suspend fun getAllFaviconUris(): List<String> = dao.getAllFavicons()
}
