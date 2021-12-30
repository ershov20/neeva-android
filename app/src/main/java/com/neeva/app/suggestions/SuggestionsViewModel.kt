package com.neeva.app.suggestions

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.coroutines.await
import com.neeva.app.NeevaBrowser
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.apolloClient
import com.neeva.app.browsing.baseDomain
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.Site
import com.neeva.app.type.QuerySuggestionType
import com.neeva.app.urlbar.URLBarModel
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
    val annotationType: AnnotationType = AnnotationType.Default,
    val imageURL: String? = null,
    val stockInfo: SuggestionsQuery.StockInfo? = null,
    val dictionaryInfo: SuggestionsQuery.DictionaryInfo? = null
)

data class Suggestions(
    val autocompleteSuggestion: NavSuggestion? = null,
    val queryRowSuggestions: List<QueryRowSuggestion> = emptyList(),
    val navSuggestions: List<NavSuggestion> = emptyList()
)

/** Maintains a list of suggestions from the backend that correspond to the current query. */
class SuggestionsViewModel(
    historyViewModel: HistoryViewModel,
    urlBarModel: URLBarModel
): ViewModel() {
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

        viewModelScope.launch {
            // Every time the contents of the URL bar changes, try to fire a query to the backend.
            urlBarModel.text.collect { textEditValue ->
                // Because the URL bar text changes whenever the user navigates somewhere, we will
                // end up firing a wasted query at the backend.  Make sure the user is actively
                // typing something in.
                if (!urlBarModel.isEditing.value) return@collect

                // If the query is blank, don't bother firing the query.
                var result: SuggestionsQuery.Suggest? = null
                if (textEditValue.text.isNotBlank()) {
                    val response = apolloClient(NeevaBrowser.context)
                        .query(SuggestionsQuery(query = textEditValue.text))
                        .await()
                    result = response.data?.suggest
                }

                updateWith(result)
            }
        }
    }

    private fun updateWith(suggestionResults: SuggestionsQuery.Suggest?) {
        if (suggestionResults == null) {
            // Clear out all the suggestions.
            _suggestionFlow.value = Suggestions(
                autocompleteSuggestion = autocompleteSuggestion.value
            )
            return
        }

        val urlSuggestionsSplit = suggestionResults.urlSuggestion
            .partition { !it.subtitle.isNullOrEmpty() && !it.title.isNullOrEmpty() }
        val viewableSuggestions = urlSuggestionsSplit.first
        _suggestionFlow.value = Suggestions(
            autocompleteSuggestion = autocompleteSuggestion.value,
            queryRowSuggestions = suggestionResults.querySuggestion.map { it.toQueryRowSuggestion() },
            navSuggestions = viewableSuggestions.map { it.toNavSuggestion() }
        )
    }

    class SuggestionsViewModelFactory(
        private val historyViewModel: HistoryViewModel,
        private val urlBarModel: URLBarModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SuggestionsViewModel(historyViewModel, urlBarModel) as T
        }
    }
}

fun SuggestionsQuery.UrlSuggestion.toNavSuggestion() = NavSuggestion(
    url = Uri.parse(suggestedURL),
    label = subtitle ?: "",
    secondaryLabel = title?.let { Uri.parse(it).baseDomain() } ?: "",
    queryIndex = sourceQueryIndex
)

fun SuggestionsQuery.QuerySuggestion.toQueryRowSuggestion() = QueryRowSuggestion(
    url = this.suggestedQuery.toSearchUri(),
    query = this.suggestedQuery,
    drawableID = when {
        this.type == QuerySuggestionType.STANDARD -> R.drawable.ic_baseline_search_24
        this.type == QuerySuggestionType.SEARCHHISTORY -> R.drawable.ic_baseline_history_24
        !this.annotation?.description.isNullOrEmpty() -> R.drawable.ic_baseline_image_24
        else -> R.drawable.ic_baseline_search_24
    },
    description = this.annotation?.description,
    imageURL = this.annotation?.imageURL,
    annotationType = AnnotationType.fromString(this.annotation?.annotationType),
    stockInfo = this.annotation?.stockInfo,
    dictionaryInfo = this.annotation?.dictionaryInfo
)

fun Site.toNavSuggestion() : NavSuggestion {
    val uri = Uri.parse(this.siteURL)

    return NavSuggestion(
        url = uri,
        label = this.metadata?.title ?: uri.baseDomain() ?: this.siteURL,
        secondaryLabel = uri.toString()
    )
}
