package com.neeva.app.suggestions

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.browsing.baseDomain
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.type.QuerySuggestionType
import java.lang.Integer.min

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

    private val _urlSuggestions = MutableLiveData<List<SuggestionsQuery.UrlSuggestion>>()
    val urlSuggestions: LiveData<List<SuggestionsQuery.UrlSuggestion>> = _urlSuggestions

    private val _topSuggestion = MutableLiveData<NavSuggestion?>()
    val topSuggestion: LiveData<NavSuggestion?> = _topSuggestion

    private val _navSuggestions = MutableLiveData<List<NavSuggestion>>()
    val navSuggestions: LiveData<List<NavSuggestion>> = _navSuggestions

    private val _queryRowSuggestions = MutableLiveData<List<QueryRowSuggestion>>()
    val queryRowSuggestions: LiveData<List<QueryRowSuggestion>> = _queryRowSuggestions

    private val _shouldShowSuggestions = MutableLiveData<Boolean>(false)
    val shouldShowSuggestions: LiveData<Boolean> = _shouldShowSuggestions

    fun updateWith(suggestionResults: SuggestionsQuery.Suggest) {
        suggestionResponse = suggestionResults
        val urlSuggestionsSplit = suggestionResults.urlSuggestion
            .partition { !it.subtitle.isNullOrEmpty() && !it.title.isNullOrEmpty() }
        val viewableSuggestions = urlSuggestionsSplit.first

        _navSuggestions.value = viewableSuggestions.map { it.toNavSuggestion() }
        _topSuggestion.value = null

        _queryRowSuggestions.value = suggestionResults.querySuggestion
            .map { it.toQueryRowSuggestion() }
            .subList(0, min(suggestionResults.querySuggestion.size, 3))

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
