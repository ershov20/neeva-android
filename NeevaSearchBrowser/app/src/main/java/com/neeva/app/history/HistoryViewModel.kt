package com.neeva.app.history

import android.net.Uri
import androidx.lifecycle.*
import com.neeva.app.NeevaConstants.appSearchURL
import com.neeva.app.R
import com.neeva.app.browsing.baseDomain
import com.neeva.app.storage.Favicon
import com.neeva.app.storage.Site
import com.neeva.app.storage.SitesRepository
import com.neeva.app.storage.Visit
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.QueryRowSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class HistoryViewModel(private val repository: SitesRepository) : ViewModel() {
    companion object {
        private const val MAX_FREQUENT_SITES = 40

        private val HISTORY_WINDOW = TimeUnit.DAYS.toMillis(7)
        private val HISTORY_START_DATE = Date(System.currentTimeMillis() - HISTORY_WINDOW)
    }

    val allDomains: LiveData<List<Site>> = repository.allSites.asLiveData()
    val allVisits: LiveData<List<Visit>> = repository.allVisits.asLiveData()

    /**
     * Tracks all history from |HISTORY_START_DATE| going forward.  While HISTORY_START_DATE is a
     * constant value that won't be updated until the app is reopened, we can manually filter the
     * list later with the actual timeframes we're interested in.
     */
    val historyWithinRange: Flow<List<Site>> =
        repository.getHistoryAfter(HISTORY_START_DATE)

    val frequentSites: Flow<List<Site>> =
        repository.getFrequentSitesAfter(HISTORY_START_DATE, MAX_FREQUENT_SITES)

    fun insert(
        url: Uri,
        title: String? = null,
        favicon: Favicon? = null,
        visit: Visit? = null
    ) {
        viewModelScope.launch {
            repository.insert(url, title, favicon, visit)
        }
    }
}

fun Site.toNavSuggestion() : NavSuggestion {
    val uri = Uri.parse(this.siteURL)

    return NavSuggestion(
        url = uri,
        label = this.metadata?.title ?: uri.baseDomain() ?: this.siteURL,
        secondaryLabel = uri.toString()
    )
}

fun Site.toSearchSuggest() : QueryRowSuggestion? {
    if (!siteURL.startsWith(appSearchURL)) return null
    val query = Uri.parse(this.siteURL).getQueryParameter("q") ?: return null

    return QueryRowSuggestion(
        url = Uri.parse(this.siteURL),
        query =  query,
        drawableID = R.drawable.ic_baseline_history_24
    )
}

class HistoryViewModelFactory(private val repository: SitesRepository) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(repository) as T
    }
}