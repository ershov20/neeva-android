package com.neeva.app.suggestions

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.type.QuerySuggestionType
import com.neeva.app.browsing.baseDomain
import com.neeva.app.browsing.toSearchUri
import java.lang.Integer.min

class NavSuggestion(val url: Uri, val label: String, val secondaryLabel: String)
class ChipSuggestion(val url: Uri, val query: String)
class QueryRowSuggestion(
    val url: Uri, val query: String, val description: String? = null,
    val imageURL: String? = null, val drawableID: Int)

class SuggestionsViewModel: ViewModel() {
    private var _suggestionResponse: SuggestionsQuery.Suggest? = null
    private val _urlSuggestions = MutableLiveData<List<SuggestionsQuery.UrlSuggestion>>()
    val urlSuggestions: LiveData<List<SuggestionsQuery.UrlSuggestion>> = _urlSuggestions
    private val _topSuggestions = MutableLiveData<List<NavSuggestion>>()
    val topSuggestions: LiveData<List<NavSuggestion>> = _topSuggestions
    private val _navSuggestions = MutableLiveData<List<NavSuggestion>>()
    val navSuggestions: LiveData<List<NavSuggestion>> = _navSuggestions
    private val _queryChipSuggestions = MutableLiveData<List<ChipSuggestion>>()
    val queryChipSuggestions: LiveData<List<ChipSuggestion>> = _queryChipSuggestions
    private val _queryRowSuggestions = MutableLiveData<List<QueryRowSuggestion>>()
    val queryRowSuggestions: LiveData<List<QueryRowSuggestion>> = _queryRowSuggestions

    private val _shouldShowSuggestions = MutableLiveData<Boolean>(false)
    val shouldShowSuggestions: LiveData<Boolean> = _shouldShowSuggestions

    fun updateWith(suggestionResults: SuggestionsQuery.Suggest) {
        _suggestionResponse = suggestionResults
        val urlSuggestionsSplit = suggestionResults.urlSuggestion
            .partition { !it.subtitle.isNullOrEmpty() && !it.title.isNullOrEmpty() }
        _navSuggestions.value = urlSuggestionsSplit.first
            .drop(1).map { it.toNavSuggestion() }
        if (urlSuggestionsSplit.first.isNotEmpty()) {
            _topSuggestions.value = mutableListOf(
                urlSuggestionsSplit.first.first().toNavSuggestion())
        }
        _queryRowSuggestions.value = suggestionResults.querySuggestion
            .map { it.toQueryRowSuggestion() }.subList(0, min(suggestionResults.querySuggestion.size, 3))
        _shouldShowSuggestions.value = !_suggestionResponse?.urlSuggestion.isNullOrEmpty()
                || !_suggestionResponse?.querySuggestion.isNullOrEmpty()
    }
}

fun SuggestionsQuery.UrlSuggestion.toNavSuggestion() : NavSuggestion =
    NavSuggestion(url = Uri.parse(this.suggestedURL),label = this.subtitle!!,
        secondaryLabel = Uri.parse(this.title!!).baseDomain() ?: this.title!!)

fun SuggestionsQuery.QuerySuggestion.toChipSuggestion() : ChipSuggestion =
    ChipSuggestion(this.suggestedQuery.toSearchUri(), this.suggestedQuery)

fun SuggestionsQuery.QuerySuggestion.toQueryRowSuggestion() : QueryRowSuggestion {
    return QueryRowSuggestion(
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
}
