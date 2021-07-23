package com.neeva.app.suggestions

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neeva.app.R
import com.neeva.app.SuggestionsQuery
import com.neeva.app.type.QuerySuggestionType
import com.neeva.app.web.toSearchUrl

class NavSuggestion(val url: String, val label: String, val secondaryLabel: String)
class ChipSuggestion(val url: String, val query: String)
class QueryRowSuggestion(
    val url: String, val query: String, val description: String?,
    val imageURL: String?, val drawableID: Int)

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
            .partition { it.subtitle?.isNotEmpty() ?: false }
        _navSuggestions.value = urlSuggestionsSplit.first.drop(1)
            .filter { !it.subtitle.isNullOrEmpty() && !it.title.isNullOrEmpty() }
            .map {
            NavSuggestion(url = it.suggestedURL,label = it.subtitle!!,
                secondaryLabel = it.title!!.parseBaseDomain())
        }
        _urlSuggestions.value = urlSuggestionsSplit.second
        if (urlSuggestionsSplit.first.isNotEmpty()) {
            val topSuggestion = urlSuggestionsSplit.first.first()
            if (topSuggestion.title.isNullOrEmpty() || topSuggestion.subtitle.isNullOrEmpty()) {
                return
            }
            _topSuggestions.value = mutableListOf(NavSuggestion(
                url = topSuggestion.suggestedURL,
                label = topSuggestion.subtitle,
                secondaryLabel = topSuggestion.title.parseBaseDomain()))
        }
        val querySuggestionsSplit = suggestionResults.querySuggestion
            .partition { it.type != QuerySuggestionType.STANDARD
                    || it.annotation?.description?.isNotEmpty() == true }
        _queryRowSuggestions.value = querySuggestionsSplit.first
            .map { it.toQueryRowSuggestion() }
        _queryChipSuggestions.value = querySuggestionsSplit.second
            .map { it.toChipSuggestion() }
        _shouldShowSuggestions.value = !_suggestionResponse?.urlSuggestion.isNullOrEmpty()
                || !_suggestionResponse?.querySuggestion.isNullOrEmpty()
    }
}

fun String.parseBaseDomain(): String {
    // if this is a url, compute the authority and drop www if needed.
    var authority =  Uri.parse(this).authority ?: this
    if (authority.startsWith("www")) {
        authority = authority.substring(4)
    }
    return authority
}

fun SuggestionsQuery.QuerySuggestion.toChipSuggestion() : ChipSuggestion =
    ChipSuggestion(this.suggestedQuery.toSearchUrl(), this.suggestedQuery)

fun SuggestionsQuery.QuerySuggestion.toQueryRowSuggestion() : QueryRowSuggestion {
    return QueryRowSuggestion(
        url = this.suggestedQuery.toSearchUrl(),
        query = this.suggestedQuery,
        description = this.annotation?.description,
        imageURL = this.annotation?.imageURL,
        drawableID = when {
            this.type == QuerySuggestionType.STANDARD -> R.drawable.ic_baseline_search_24
            this.type == QuerySuggestionType.SEARCHHISTORY -> R.drawable.ic_baseline_history_24
            !this.annotation?.description.isNullOrEmpty() -> R.drawable.ic_baseline_image_24
            else -> R.drawable.ic_baseline_search_24
        }
    )
}
