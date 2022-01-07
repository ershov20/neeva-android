package com.neeva.app.history

import android.net.Uri
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.Domain
import com.neeva.app.storage.DomainRepository
import com.neeva.app.storage.Favicon
import com.neeva.app.storage.Favicon.Companion.toFavicon
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.Site
import com.neeva.app.storage.SitesRepository
import com.neeva.app.storage.Visit
import com.neeva.app.suggestions.NavSuggestion
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
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

        private val HISTORY_WINDOW = TimeUnit.DAYS.toMillis(7)
        private val HISTORY_START_DATE = Date(System.currentTimeMillis() - HISTORY_WINDOW)
    }

    /**
     * Tracks all history from |HISTORY_START_DATE| going forward.  While HISTORY_START_DATE is a
     * constant value that won't be updated until the app is reopened, we can manually filter the
     * list later with the actual timeframes we're interested in.
     */
    val historyWithinRange: Flow<List<Site>> =
        sitesRepository.getHistoryAfter(HISTORY_START_DATE)

    val frequentSites: Flow<List<Site>> =
        sitesRepository.getFrequentSitesAfter(HISTORY_START_DATE, MAX_FREQUENT_SITES)

    private val _siteSuggestions = MutableStateFlow<List<Site>>(emptyList())
    val siteSuggestions: StateFlow<List<Site>> = _siteSuggestions

    private val _domainSuggestions = MutableStateFlow<List<NavSuggestion>>(emptyList())
    val domainSuggestions: StateFlow<List<NavSuggestion>> = _domainSuggestions

    /** Updates the query that is being used to fetch history and domain name suggestions. */
    fun updateSuggestionQuery(coroutineScope: CoroutineScope, currentInput: String) {
        coroutineScope.launch(Dispatchers.IO) {
            _siteSuggestions.value = sitesRepository.getQuerySuggestions(currentInput)
            _domainSuggestions.value = domainRepository.queryNavSuggestions(currentInput)
        }
    }

    /**
     * Returns a Flow that emits the best Favicon for the given Uri.  It prefers favicons that come
     * from a direct match in the user's history before falling back to the registered domain.  If
     * both of those fail and [allowFallbackIcon] is true, then a fallback favicon is provided.
     */
    fun getFaviconFlow(uri: Uri?, allowFallbackIcon: Boolean = true): Flow<Favicon?> {
        val fallbackIcon: Favicon? by lazy {
            if (!allowFallbackIcon) return@lazy null

            // Create a favicon based off the URL and the first letter of the registered domain.
            uri
                ?.let {
                    val registeredDomain = domainProvider.getRegisteredDomain(it)
                    Uri.Builder().scheme(it.scheme).authority(registeredDomain).build()
                }
                .toFavicon()
        }

        if (uri == null || uri.toString().isBlank()) return flowOf(fallbackIcon)

        val siteFlow = sitesRepository.getFlow(uri)
        val domainFlow = domainProvider.getRegisteredDomain(uri)
            ?.let { domainRepository.getFlow(it) }
            ?: flowOf(null)

        return siteFlow
            .combine(domainFlow) { site, domain ->
                site?.largestFavicon ?: domain?.largestFavicon ?: fallbackIcon
            }
            .distinctUntilChanged()
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
