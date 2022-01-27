package com.neeva.app.history

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.neeva.app.NeevaConstants
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.HistoryDatabase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Provides access to the user's navigation history. */
class HistoryManager(
    historyDatabase: HistoryDatabase,
    private val domainProvider: DomainProvider,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val MAX_FREQUENT_SITES = 40
        private const val PAGE_SIZE = 10

        private val HISTORY_WINDOW = TimeUnit.DAYS.toMillis(7)
        private val HISTORY_START_DATE = Date(System.currentTimeMillis() - HISTORY_WINDOW)
    }

    private val dao = historyDatabase.dao()

    fun getHistoryBetween(startTime: Date, endTime: Date): Flow<PagingData<Site>> {
        return Pager(PagingConfig(pageSize = PAGE_SIZE)) {
            dao.getPagedSitesVisitedBetween(startTime, endTime)
        }.flow
    }

    fun getHistoryAfter(startTime: Date): Flow<PagingData<Site>> {
        return Pager(PagingConfig(pageSize = PAGE_SIZE)) {
            dao.getPagedSitesVisitedAfter(startTime)
        }.flow
    }

    private val frequentSites: Flow<List<Site>> =
        dao.getFrequentSitesAfterFlow(HISTORY_START_DATE, MAX_FREQUENT_SITES)

    private val _siteSuggestions = MutableStateFlow<List<Site>>(emptyList())
    val siteSuggestions: StateFlow<List<Site>> = _siteSuggestions

    private val _historySuggestions = MutableStateFlow<List<NavSuggestion>>(emptyList())
    val historySuggestions: StateFlow<List<NavSuggestion>> = _historySuggestions

    /** Provides the top 3 suggestions based on how often a user visited a site. */
    val suggestedQueries: Flow<List<QueryRowSuggestion>> =
        frequentSites.map { siteList -> siteList.mapNotNull { it.toSearchSuggest() }.take(3) }

    /** Provides non-Neeva sites from history as suggestions. */
    val suggestedSites: Flow<List<Site>> =
        frequentSites
            .map { sites ->
                // Assume that anything pointing at neeva.com should not be recommended to the user.
                // This includes search suggestions and Spaces, e.g.
                sites.filterNot {
                    val registeredDomain = domainProvider.getRegisteredDomain(Uri.parse(it.siteURL))
                    registeredDomain == NeevaConstants.appHost
                }
            }

    /** Updates the query that is being used to fetch history suggestions. */
    suspend fun updateSuggestionQuery(currentInput: String?) {
        val siteSuggestions = if (currentInput != null) {
            dao.getQuerySuggestions(currentInput, limit = 10)
        } else {
            emptyList()
        }

        _siteSuggestions.value = siteSuggestions

        // Determine what history should be suggested as the user types out a query.
        val combinedSuggestions = _siteSuggestions.value.map { it.toNavSuggestion(domainProvider) }

        // Keep only the unique history items with unique URLs.
        _historySuggestions.value = combinedSuggestions.distinctBy { it.url }
    }

    /** Returns the favicon that corresponds to an exact visit in the user's history. */
    suspend fun getFaviconFromHistory(uri: Uri): Favicon? {
        val siteFavicon = dao.getSiteByUrl(uri)?.largestFavicon
        if (siteFavicon != null) return siteFavicon
        return null
    }

    /** Inserts or updates an item into the history. */
    fun upsert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            dao.upsert(url, title, favicon, visit)
        }
    }

    fun clearAllHistory() {
        coroutineScope.launch(Dispatchers.IO) {
            dao.deleteHistoryWithinTimeframe(Date(0L), Date())

            // TODO(dan.alcantara): Delete favicons.
            // TODO(dan.alcantara): Delete tab thumbnails
            // TDOO(dan.alcantara): Should we close all tabs when the user clears history?
        }
    }
}
