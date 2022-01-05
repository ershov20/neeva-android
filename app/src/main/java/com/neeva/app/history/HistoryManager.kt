package com.neeva.app.history

import android.net.Uri
import com.neeva.app.browsing.baseDomain
import com.neeva.app.storage.*
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Provides access to the user's navigation history. */
@Singleton
class HistoryManager @Inject constructor(historyDatabase: HistoryDatabase) {
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

    /** Returns a Flow that emits the best Favicon for the given Uri. */
    fun getFaviconFlow(uri: Uri?): Flow<Favicon?> {
        val domainName = uri?.baseDomain() ?: return flowOf(null)
        return domainRepository
            .listen(domainName)
            .map { it.largestFavicon }
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
        coroutineScope.launch {
            sitesRepository.insert(url, title, favicon, visit)

            url.baseDomain()?.let { domainName ->
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

    /** Updates the [Favicon] for the given [url]. */
    fun updateFaviconFor(coroutineScope: CoroutineScope, url: String, favicon: Favicon) {
        coroutineScope.launch {
            domainRepository.updateFaviconFor(url, favicon)
        }
    }
}
