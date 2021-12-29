package com.neeva.app.suggestions

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.browsing.baseDomain
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.storage.Site
import com.neeva.app.type.QuerySuggestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NavSuggestion(
    val url: Uri,
    val label: String,
    val secondaryLabel: String,
    val queryIndex: Int? = null
)

data class ChipSuggestion(
    val url: Uri,
    val query: String
)

data class QueryRowSuggestion(
    val url: Uri,
    val query: String,
    val description: String? = null,
    val imageURL: String? = null,
    val drawableID: Int
)

class SuggestionsViewModel: ViewModel() {
    private var suggestionResponse: SuggestionsQuery.Suggest? = null

    private val _urlSuggestions = MutableStateFlow<List<SuggestionsQuery.UrlSuggestion>>(emptyList())
    val urlSuggestions: StateFlow<List<SuggestionsQuery.UrlSuggestion>> = _urlSuggestions

    private val _topSuggestion = MutableStateFlow<NavSuggestion?>(null)
    val topSuggestion: StateFlow<NavSuggestion?> = _topSuggestion

    private val _navSuggestions = MutableStateFlow<List<NavSuggestion>>(emptyList())
    val navSuggestions: StateFlow<List<NavSuggestion>> = _navSuggestions

    private val _queryRowSuggestions = MutableStateFlow<List<QueryRowSuggestion>>(emptyList())
    val queryRowSuggestions: StateFlow<List<QueryRowSuggestion>> = _queryRowSuggestions

    private val _shouldShowSuggestions = MutableStateFlow(false)
    val shouldShowSuggestions: StateFlow<Boolean> = _shouldShowSuggestions

    fun updateWith(suggestionResults: SuggestionsQuery.Suggest) {
        suggestionResponse = suggestionResults
        val urlSuggestionsSplit = suggestionResults.urlSuggestion
            .partition { !it.subtitle.isNullOrEmpty() && !it.title.isNullOrEmpty() }
        val viewableSuggestions = urlSuggestionsSplit.first

        _navSuggestions.value = viewableSuggestions.map { it.toNavSuggestion() }
        _topSuggestion.value = null

        _queryRowSuggestions.value = suggestionResults.querySuggestion
            .map { it.toQueryRowSuggestion() }

        _shouldShowSuggestions.value = !suggestionResponse?.urlSuggestion.isNullOrEmpty()
                || !suggestionResponse?.querySuggestion.isNullOrEmpty()
    }
}

fun SuggestionsQuery.UrlSuggestion.toNavSuggestion() = NavSuggestion(
    url = Uri.parse(suggestedURL),
    label = subtitle!!,
    secondaryLabel = Uri.parse(title!!).baseDomain() ?: title,
    queryIndex = sourceQueryIndex
)

fun SuggestionsQuery.QuerySuggestion.toChipSuggestion() = ChipSuggestion(
    suggestedQuery.toSearchUri(),
    suggestedQuery
)

fun SuggestionsQuery.QuerySuggestion.toQueryRowSuggestion() = QueryRowSuggestion(
    url = this.suggestedQuery.toSearchUri(),
    query = this.suggestedQuery,
    description = this.annotation?.description,
    imageURL = this.annotation?.imageURL,
    drawableID = when {
        this.annotation?.annotationType == "Calculator" -> R.drawable.ic_baseline_calculate_24
        this.type == QuerySuggestionType.STANDARD -> R.drawable.ic_baseline_search_24
        this.type == QuerySuggestionType.SEARCHHISTORY -> R.drawable.ic_baseline_history_24
        !this.annotation?.description.isNullOrEmpty() -> R.drawable.ic_baseline_image_24
        else -> R.drawable.ic_baseline_search_24
    }
)

fun Site.toNavSuggestion() : NavSuggestion {
    val uri = Uri.parse(this.siteURL)

    return NavSuggestion(
        url = uri,
        label = this.metadata?.title ?: uri.baseDomain() ?: this.siteURL,
        secondaryLabel = uri.toString()
    )
}
