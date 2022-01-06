package com.neeva.app.suggestions

import android.net.Uri
import android.util.Log
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.browsing.baseDomain
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.history.HistoryManager
import com.neeva.app.storage.Site
import com.neeva.app.type.QuerySuggestionType
import com.neeva.app.urlbar.URLBarModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
class SuggestionsModel(
    coroutineScope: CoroutineScope,
    historyManager: HistoryManager,
    private val urlBarModel: URLBarModel,
    private val apolloClient: ApolloClient
) {
    companion object {
        val TAG = SuggestionsModel::class.simpleName
    }

    private val _suggestionFlow = MutableStateFlow(Suggestions())
    val suggestionFlow: StateFlow<Suggestions> = _suggestionFlow

    private val _autocompleteSuggestion = MutableStateFlow<NavSuggestion?>(null)
    val autocompleteSuggestion: StateFlow<NavSuggestion?> = _autocompleteSuggestion

    init {
        coroutineScope.launch {
            historyManager.siteSuggestions.collect { suggestions ->
                _autocompleteSuggestion.value = suggestions.firstOrNull()?.toNavSuggestion()

                _suggestionFlow.value = _suggestionFlow.value.copy(
                    autocompleteSuggestion = _autocompleteSuggestion.value
                )
            }
        }

        coroutineScope.launch {
            // Every time the contents of the URL bar changes, try to fire a query to the backend.
            urlBarModel.userInputText
                .combine(urlBarModel.isEditing) { textFieldValue, isEditing ->
                    Pair(textFieldValue, isEditing)
                }
                .collect { (urlBarValue, isEditing) ->
                    onUrlBarChanged(urlBarValue.text, isEditing)
                }
        }
    }

    internal suspend fun onUrlBarChanged(newValue: String, isEditing: Boolean) {
        // Because the URL bar text changes whenever the user navigates somewhere, we will end up
        // firing a wasted query.  Make sure the user is actively typing something in.
        if (!isEditing) return

        // If the query is blank, don't bother firing the query.
        var result: SuggestionsQuery.Data? = null
        if (newValue.isNotBlank()) {
            val query = apolloClient.query(SuggestionsQuery(query = newValue))

            try {
                val response = query.execute()
                result = response.data

                if (result == null || response.hasErrors()) {
                    Log.e(TAG, "Failed to parse response.  Has errors: ${response.hasErrors()}")
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Caught runtime exception while performing query.  Removing suggestions", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform query.  Removing suggestions", e)
            }
        }

        updateWith(result)
    }

    private fun updateWith(suggestionResultsData: SuggestionsQuery.Data?) {
        val suggestionResults = suggestionResultsData?.suggest
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
        this.type == QuerySuggestionType.Standard -> R.drawable.ic_baseline_search_24
        this.type == QuerySuggestionType.SearchHistory -> R.drawable.ic_baseline_history_24
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
