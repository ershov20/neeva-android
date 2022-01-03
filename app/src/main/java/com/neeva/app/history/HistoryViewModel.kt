package com.neeva.app.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neeva.app.browsing.baseDomain
import com.neeva.app.storage.*
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

/** Provides access to the user's navigation history. */
class HistoryViewModel(
    private val sitesRepository: SitesRepository,
    private val domainRepository: DomainRepository
) : ViewModel() {
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
    fun updateSuggestionQuery(currentInput: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) {
        viewModelScope.launch {
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
    fun updateFaviconFor(url: String, favicon: Favicon) {
        viewModelScope.launch {
            domainRepository.updateFaviconFor(url, favicon)
        }
    }

    class HistoryViewModelFactory(
        private val sitesRepository: SitesRepository,
        private val domainRepository: DomainRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(sitesRepository, domainRepository) as T
        }
    }
}
