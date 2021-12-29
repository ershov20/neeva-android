package com.neeva.app.suggestions

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.browsing.baseDomain
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.Site
import com.neeva.app.type.QuerySuggestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class NavSuggestion(
    val url: Uri,
    val label: String,
    val secondaryLabel: String,
    val queryIndex: Int? = null
)

data class QueryRowSuggestion(
    val url: Uri,
    val query: String,
    val drawableID: Int,
    val description: String? = null,
    val imageURL: String? = null
)

data class Suggestions(
    val autocompleteSuggestion: NavSuggestion? = null,
    val queryRowSuggestions: List<QueryRowSuggestion> = emptyList(),
    val navSuggestions: List<NavSuggestion> = emptyList(),
    val shouldShowSuggestions: Boolean = false
)

/** Maintains a list of suggestions from the backend that correspond to the current query. */
class SuggestionsViewModel(
    historyViewModel: HistoryViewModel
): ViewModel() {
    private var suggestionResponse: SuggestionsQuery.Suggest? = null

    private val _suggestionFlow = MutableStateFlow(Suggestions())
    val suggestionFlow: StateFlow<Suggestions> = _suggestionFlow

    private val _autocompleteSuggestion = MutableStateFlow<NavSuggestion?>(null)
    val autocompleteSuggestion: StateFlow<NavSuggestion?> = _autocompleteSuggestion

    init {
        viewModelScope.launch {
            historyViewModel.siteSuggestions.collect { suggestions ->
                _autocompleteSuggestion.value = suggestions.firstOrNull()?.toNavSuggestion()

                _suggestionFlow.value = _suggestionFlow.value.copy(
                    autocompleteSuggestion = _autocompleteSuggestion.value
                )
            }
        }
    }

    fun updateWith(suggestionResults: SuggestionsQuery.Suggest) {
        suggestionResponse = suggestionResults
        val urlSuggestionsSplit = suggestionResults.urlSuggestion
            .partition { !it.subtitle.isNullOrEmpty() && !it.title.isNullOrEmpty() }
        val viewableSuggestions = urlSuggestionsSplit.first

        _suggestionFlow.value = Suggestions(
            autocompleteSuggestion = autocompleteSuggestion.value,
            queryRowSuggestions = suggestionResults.querySuggestion.map { it.toQueryRowSuggestion() },
            navSuggestions = viewableSuggestions.map { it.toNavSuggestion() },
            shouldShowSuggestions =
                !suggestionResponse?.urlSuggestion.isNullOrEmpty()
                    || !suggestionResponse?.querySuggestion.isNullOrEmpty()
        )
    }

    class SuggestionsViewModelFactory(
        private val historyViewModel: HistoryViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SuggestionsViewModel(historyViewModel) as T
        }
    }
}

fun SuggestionsQuery.UrlSuggestion.toNavSuggestion() = NavSuggestion(
    url = Uri.parse(suggestedURL),
    label = subtitle!!,
    secondaryLabel = Uri.parse(title!!).baseDomain() ?: title,
    queryIndex = sourceQueryIndex
)

fun SuggestionsQuery.QuerySuggestion.toQueryRowSuggestion() = QueryRowSuggestion(
    url = this.suggestedQuery.toSearchUri(),
    query = this.suggestedQuery,
    drawableID = when {
        this.annotation?.annotationType == "Calculator" -> R.drawable.ic_baseline_calculate_24
        this.type == QuerySuggestionType.STANDARD -> R.drawable.ic_baseline_search_24
        this.type == QuerySuggestionType.SEARCHHISTORY -> R.drawable.ic_baseline_history_24
        !this.annotation?.description.isNullOrEmpty() -> R.drawable.ic_baseline_image_24
        else -> R.drawable.ic_baseline_search_24
    },
    description = this.annotation?.description,
    imageURL = this.annotation?.imageURL
)

fun Site.toNavSuggestion() : NavSuggestion {
    val uri = Uri.parse(this.siteURL)

    return NavSuggestion(
        url = uri,
        label = this.metadata?.title ?: uri.baseDomain() ?: this.siteURL,
        secondaryLabel = uri.toString()
    )
}
