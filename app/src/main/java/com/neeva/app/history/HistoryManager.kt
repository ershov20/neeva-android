package com.neeva.app.history

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.neeva.app.NeevaConstants
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.Domain
import com.neeva.app.storage.DomainRepository
import com.neeva.app.storage.Favicon
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.Site
import com.neeva.app.storage.SitesRepository
import com.neeva.app.storage.Visit
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
import kotlinx.coroutines.withContext

/** Provides access to the user's navigation history. */
class HistoryManager(
    historyDatabase: HistoryDatabase,
    private val domainProvider: DomainProvider
) {
    private val sitesRepository = SitesRepository(historyDatabase.fromSites())
    private val domainRepository = DomainRepository(historyDatabase.fromDomains())

    companion object {
        private const val MAX_FREQUENT_SITES = 40
        private const val PAGE_SIZE = 10

        private val HISTORY_WINDOW = TimeUnit.DAYS.toMillis(7)
        private val HISTORY_START_DATE = Date(System.currentTimeMillis() - HISTORY_WINDOW)
    }

    fun getHistoryBetween(startTime: Date, endTime: Date): Flow<PagingData<Site>> {
        return Pager(PagingConfig(pageSize = PAGE_SIZE)) {
            sitesRepository.getHistoryBetween(startTime, endTime)
        }.flow
    }

    fun getHistoryAfter(startTime: Date): Flow<PagingData<Site>> {
        return Pager(PagingConfig(pageSize = PAGE_SIZE)) {
            sitesRepository.getHistoryAfter(startTime)
        }.flow
    }

    private val frequentSites: Flow<List<Site>> =
        sitesRepository.getFrequentSitesAfter(HISTORY_START_DATE, MAX_FREQUENT_SITES)

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

    /** Updates the query that is being used to fetch history and domain name suggestions. */
    fun updateSuggestionQuery(coroutineScope: CoroutineScope, currentInput: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val siteSuggestions = sitesRepository.getQuerySuggestions(currentInput, limit = 10)
            val domainSuggestions = domainRepository.queryNavSuggestions(currentInput, limit = 10)

            _siteSuggestions.value = siteSuggestions

            // Determine what history should be suggested as the user types out a query.
            // Prioritize the site visits first because they were directly visited by the user.
            val combinedSuggestions =
                _siteSuggestions.value.map { it.toNavSuggestion(domainProvider) } +
                    domainSuggestions

            // Keep only the unique history items with unique URLs.
            _historySuggestions.value = combinedSuggestions.distinctBy { it.url }
        }
    }

    /**
     * Returns the best Favicon for the given Uri.  It prefers favicons that come from a direct
     * match in the user's history before falling back to the registered domain.
     */
    suspend fun getFaviconFromHistory(uri: Uri): Favicon? {
        val siteFavicon = sitesRepository.find(uri)?.largestFavicon
        if (siteFavicon != null) return siteFavicon

        return domainProvider
            .getRegisteredDomain(uri)
            ?.let { domainRepository.get(it) }
            ?.largestFavicon
    }

    /** Inserts or updates an item into the history. */
    fun insert(
        coroutineScope: CoroutineScope,
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            sitesRepository.insert(url, title, favicon, visit)

            domainProvider.getRegisteredDomain(url)?.let { domainName ->
                domainRepository.insert(
                    Domain(
                        domainName = domainName,
                        providerName = title,
                        largestFavicon = null
                    )
                )
            }
        }
    }

    /** Updates the [Favicon] for the domain associated with the given [url]. */
    suspend fun updateDomainFavicon(url: String, favicon: Favicon?) {
        favicon ?: return

        withContext(Dispatchers.IO) {
            domainRepository.updateFaviconFor(
                domainProvider.getRegisteredDomain(Uri.parse(url)) ?: return@withContext,
                favicon
            )
        }
    }
}
