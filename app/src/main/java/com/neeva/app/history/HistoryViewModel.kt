package com.neeva.app.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neeva.app.storage.Favicon
import com.neeva.app.storage.Site
import com.neeva.app.storage.SitesRepository
import com.neeva.app.storage.Visit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Provides access to the user's navigation history.
 *
 * TODO(dan.alcantara): Should this be combined with DomainViewModel?  Seems like they're both
 *                      dealing with history in some way.
 */
class HistoryViewModel(private val repository: SitesRepository) : ViewModel() {
    companion object {
        private const val MAX_FREQUENT_SITES = 40

        private val HISTORY_WINDOW = TimeUnit.DAYS.toMillis(7)
        private val HISTORY_START_DATE = Date(System.currentTimeMillis() - HISTORY_WINDOW)

        class HistoryViewModelFactory(private val repository: SitesRepository) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(repository) as T
            }
        }
    }

    /**
     * Tracks all history from |HISTORY_START_DATE| going forward.  While HISTORY_START_DATE is a
     * constant value that won't be updated until the app is reopened, we can manually filter the
     * list later with the actual timeframes we're interested in.
     */
    val historyWithinRange: Flow<List<Site>> =
        repository.getHistoryAfter(HISTORY_START_DATE)

    val frequentSites: Flow<List<Site>> =
        repository.getFrequentSitesAfter(HISTORY_START_DATE, MAX_FREQUENT_SITES)

    private val _siteSuggestions = MutableStateFlow<List<Site>>(emptyList())
    val siteSuggestions: StateFlow<List<Site>> = _siteSuggestions

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

    fun updateSuggestionQuery(currentInput: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _siteSuggestions.value = repository.getQuerySuggestions(currentInput)
        }
    }
}
