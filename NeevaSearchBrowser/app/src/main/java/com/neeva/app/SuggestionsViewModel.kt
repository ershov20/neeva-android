package com.neeva.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SuggestionsViewModel: ViewModel() {
    private var _suggestionResponse: SuggestionsQuery.Suggest? = null
    private val _urlSuggestions = MutableLiveData<List<SuggestionsQuery.UrlSuggestion>>()
    val urlSuggestions: LiveData<List<SuggestionsQuery.UrlSuggestion>> = _urlSuggestions
    private val _topSuggestions = MutableLiveData<List<SuggestionsQuery.UrlSuggestion>>()
    val topSuggestions: LiveData<List<SuggestionsQuery.UrlSuggestion>> = _topSuggestions
    private val _navSuggestions = MutableLiveData<List<SuggestionsQuery.UrlSuggestion>>()
    val navSuggestions: LiveData<List<SuggestionsQuery.UrlSuggestion>> = _navSuggestions
    private val _queryChipSuggestions = MutableLiveData<List<SuggestionsQuery.QuerySuggestion>>()
    val queryChipSuggestions: LiveData<List<SuggestionsQuery.QuerySuggestion>> = _queryChipSuggestions
    private val _queryRowSuggestions = MutableLiveData<List<SuggestionsQuery.QuerySuggestion>>()
    val queryRowSuggestions: LiveData<List<SuggestionsQuery.QuerySuggestion>> = _queryRowSuggestions

    private val _shouldShowSuggestions = MutableLiveData<Boolean>(false)
    val shouldShowSuggestions: LiveData<Boolean> = _shouldShowSuggestions

    fun updateWith(suggestionResults: SuggestionsQuery.Suggest) {
        _suggestionResponse = suggestionResults
        val urlSuggestionsSplit = suggestionResults.urlSuggestion
            .partition { it.subtitle?.isNotEmpty() ?: false }
        _navSuggestions.value = urlSuggestionsSplit.first.drop(1)
        _urlSuggestions.value = urlSuggestionsSplit.second
        if (urlSuggestionsSplit.first.isNotEmpty()) {
            _topSuggestions.value = mutableListOf(urlSuggestionsSplit.first.first())
        }
        val querySuggestionsSplit = suggestionResults.querySuggestion
            .partition { it.annotation?.description?.isNotEmpty() ?: false }
        _queryRowSuggestions.value = querySuggestionsSplit.first
        _queryChipSuggestions.value = querySuggestionsSplit.second
        _shouldShowSuggestions.value = _suggestionResponse?.urlSuggestion?.isNotEmpty() ?: false
                || _suggestionResponse?.querySuggestion?.isNotEmpty() ?: false
    }
}