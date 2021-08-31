package com.neeva.app.history

import android.net.Uri
import androidx.lifecycle.*
import com.neeva.app.R
import com.neeva.app.appSearchURL
import com.neeva.app.storage.Favicon
import com.neeva.app.storage.Site
import com.neeva.app.storage.SitesRepository
import com.neeva.app.storage.Visit
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.web.baseDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit


class HistoryViewModel(private val repository: SitesRepository) : ViewModel() {
    companion object {
        private const val LIMIT_TO_FREQUENT_SITES = 40
        private val HISTORY_WINDOW = TimeUnit.DAYS.toMillis(7)
        private fun startOfDay() =
            Date.from(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC))
        private fun startOfYesterday() =
            Date.from(LocalDate.now().atStartOfDay().minusDays(1).toInstant(ZoneOffset.UTC))
        private fun startOfWeek() =
            Date.from(LocalDate.now().atStartOfDay().minusDays(6).toInstant(ZoneOffset.UTC))
    }

    val allDomains: LiveData<List<Site>> = repository.allSites.asLiveData()
    val allVisits: LiveData<List<Visit>> = repository.allVisits.asLiveData()

    // This assures we see history for the last week starting from last app start. We can emit
    // new values to this if we want different windows.
    private val historyRefresh: MutableStateFlow<Date> =
        MutableStateFlow(startOfWeek())

    // A state flow for fetching history for a designated time window
    private val historyFetch: MutableStateFlow<Pair<Date, Date>> =
        MutableStateFlow(Pair(Date(System.currentTimeMillis() - HISTORY_WINDOW), Date()))

    val history = historyRefresh.flatMapLatest { repository.getHistoryAfter(it) }.asLiveData()
    val historyToday = repository.getHistoryWithin(startOfDay(), Date()).map { historyList ->
        historyList.map { Pair(it.first.toNavSuggest(), it.second) }
    }.asLiveData()
    val historyYesterday = repository.getHistoryWithin(startOfYesterday(), startOfDay()).map { historyList ->
        historyList.map { Pair(it.first.toNavSuggest(), it.second) }
    }.asLiveData()
    val historyThisWeek = repository.getHistoryWithin(startOfWeek(), startOfYesterday()).map { historyList ->
        historyList.map { Pair(it.first.toNavSuggest(), it.second) }
    }.asLiveData()

    val frequentSites = historyRefresh.flatMapLatest {
        repository.getFrequentSitesAfter(it, LIMIT_TO_FREQUENT_SITES)
    }.asLiveData()

    fun insert(url: Uri, title: String? = null, favicon: Favicon? = null, visit: Visit? = null)
            = viewModelScope.launch {
        repository.insert(url, title, favicon, visit)
    }
}

fun Site.toNavSuggest() : NavSuggestion = NavSuggestion(
    url = Uri.parse(this.siteURL),
    label = this.metadata?.title ?: Uri.parse(this.siteURL).baseDomain() ?: this.siteURL,
    secondaryLabel = Uri.parse(this.siteURL).baseDomain() ?: this.siteURL
)

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