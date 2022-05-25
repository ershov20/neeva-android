package com.neeva.app.suggestions

import android.net.Uri
import android.util.Log
import com.neeva.app.ApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.storage.entities.Site
import com.neeva.app.type.QuerySuggestionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NavSuggestion(
    val url: Uri,
    val label: String,
    val secondaryLabel: String,
    val queryIndex: Int? = null,
    val type: SuggestionType,
    val position: Int? = null
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

enum class SuggestionType {
    AUTOCOMPLETE_SUGGESTION,
    QUERY_SUGGESTION,
    HISTORY_SUGGESTION,
    MEMORIZED_SUGGESTION,
    NO_SUGGESTION_URL,
    NO_SUGGESTION_QUERY
}

/** Maintains a list of suggestions from the backend that correspond to the current query. */
class SuggestionsModel(
    coroutineScope: CoroutineScope,
    historyManager: HistoryManager,
    val settingsDataModel: SettingsDataModel,
    private val apolloWrapper: ApolloWrapper,
    dispatchers: Dispatchers,
    val neevaConstants: NeevaConstants,
    val clientLogger: ClientLogger,
) {
    companion object {
        val TAG = SuggestionsModel::class.simpleName
    }

    private val _suggestionFlow = MutableStateFlow(Suggestions())
    val suggestionFlow: StateFlow<Suggestions> = _suggestionFlow
    val autocompleteSuggestionFlow: StateFlow<NavSuggestion?> = _suggestionFlow
        .map { it.autocompleteSuggestion }
        .distinctUntilChanged()
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    init {
        coroutineScope.launch(dispatchers.io) {
            historyManager.historySuggestions.collectLatest { suggestions ->
                // Add an autocomplete suggestion based on the top history result
                val showSearchSuggestions = settingsDataModel
                    .getSettingsToggleValue(SettingsToggle.SHOW_SEARCH_SUGGESTIONS)
                if (showSearchSuggestions) {
                    val newSuggestion = suggestions.firstOrNull()
                    _suggestionFlow.value = _suggestionFlow.value.copy(
                        autocompleteSuggestion = newSuggestion
                    )
                }
            }
        }
    }

    internal suspend fun getSuggestionsFromBackend(newValue: String?) {
        if (newValue.isNullOrBlank()) {
            updateWith(null)
            return
        }

        // If the query is blank or SHOW_SEARCH_SUGGESTIONS is off, don't bother firing the query.
        var result: SuggestionsQuery.Data? = null
        val showSearchSuggestions = settingsDataModel
            .getSettingsToggleValue(SettingsToggle.SHOW_SEARCH_SUGGESTIONS)

        if (newValue.isNotBlank() && showSearchSuggestions) {
            try {
                result = apolloWrapper.performQuery(
                    SuggestionsQuery(query = newValue),
                    userMustBeLoggedIn = false
                )?.data
            } catch (e: CancellationException) {
                // Report nothing because the Flow itself was cancelled -- probably because the user
                // continued typing something else.  Keep the old suggestions displayed.
                return
            } catch (e: Exception) {
                Log.e(TAG, "Caught exception while performing query.  Removing suggestions", e)
            }
        }

        updateWith(result)
    }

    private fun updateWith(suggestionResultsData: SuggestionsQuery.Data?) {
        val suggestionResults = suggestionResultsData?.suggest
        val queryRowSuggestions = suggestionResults?.querySuggestion
            ?.map { it.toQueryRowSuggestion(neevaConstants) }
            ?: emptyList()
        val viewableSuggestions = suggestionResults?.urlSuggestion
            ?.filter { !it.subtitle.isNullOrEmpty() && !it.title.isNullOrEmpty() }
            ?.map { it.toNavSuggestion() }
            ?: emptyList()

        _suggestionFlow.value = _suggestionFlow.value.copy(
            queryRowSuggestions = queryRowSuggestions,
            navSuggestions = viewableSuggestions
        )
    }

    fun logSuggestionTap(type: SuggestionType, position: Int?) {
        val interactionType = when (type) {
            SuggestionType.QUERY_SUGGESTION -> LogConfig.Interaction.QUERY_SUGGESTION
            SuggestionType.AUTOCOMPLETE_SUGGESTION -> LogConfig.Interaction.AUTOCOMPLETE_SUGGESTION
            SuggestionType.HISTORY_SUGGESTION -> LogConfig.Interaction.HISTORY_SUGGESTION
            SuggestionType.MEMORIZED_SUGGESTION -> LogConfig.Interaction.MEMORIZED_SUGGESTION
            SuggestionType.NO_SUGGESTION_URL -> LogConfig.Interaction.NO_SUGGESTION_URL
            SuggestionType.NO_SUGGESTION_QUERY -> LogConfig.Interaction.NO_SUGGESTION_QUERY
        }
        clientLogger.logCounter(interactionType, null)
    }
}

fun SuggestionsQuery.UrlSuggestion.toNavSuggestion() = NavSuggestion(
    url = Uri.parse(suggestedURL),
    label = subtitle ?: "",
    secondaryLabel = title ?: "",
    queryIndex = sourceQueryIndex,
    type = SuggestionType.MEMORIZED_SUGGESTION
)

fun SuggestionsQuery.QuerySuggestion.toQueryRowSuggestion(neevaConstants: NeevaConstants) =
    QueryRowSuggestion(
        url = this.suggestedQuery.toSearchUri(neevaConstants),
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

/** Generates Nav Suggestion for Sites in History. */
fun Site.toNavSuggestion(domainProvider: DomainProvider): NavSuggestion {
    val uri = Uri.parse(this.siteURL)

    return NavSuggestion(
        url = uri,
        label = toUserVisibleString(domainProvider),
        secondaryLabel = uri.toString(),
        type = SuggestionType.HISTORY_SUGGESTION
    )
}

/** Returns a string that can be displayed to the user that represents a [Site] in the UI. */
fun Site.toUserVisibleString(domainProvider: DomainProvider): String {
    val uri = Uri.parse(this.siteURL)
    return this.title.takeUnless { it.isNullOrBlank() }
        ?: domainProvider.getRegisteredDomain(uri)
        ?: this.siteURL
}
