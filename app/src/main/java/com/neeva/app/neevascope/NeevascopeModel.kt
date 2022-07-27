package com.neeva.app.neevascope

import android.net.Uri
import android.util.Log
import com.neeva.app.Dispatchers
import com.neeva.app.SearchQuery
import com.neeva.app.apollo.ApolloWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NeevascopeWebResult(
    val faviconURL: String,
    val displayURLHost: String,
    val displayURLPath: List<String>?,
    val actionURL: Uri,
    val title: String,
    val snippet: String? = null,
    val publicationDate: String? = null
)

data class NeevascopeResult(
    val webSearches: List<NeevascopeWebResult> = emptyList(),
    val relatedSearches: List<String> = emptyList()
)

class NeevascopeModel(
    private val apolloWrapper: ApolloWrapper,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers
) {
    companion object {
        val TAG = NeevascopeModel::class.simpleName
    }

    private val _searchQuery = MutableStateFlow<String?>(value = null)
    val searchQuery: StateFlow<String?> = _searchQuery
    val searchFlow: StateFlow<NeevascopeResult?> = searchQuery
        .filterNotNull()
        .map { query ->
            val searchResultsData = performNeevaScopeQuery(query = query)

            val searchResultGroups = searchResultsData?.search?.resultGroup
            val webResults: MutableList<NeevascopeWebResult> = mutableListOf()
            var relatedResults = emptyList<String>()

            searchResultGroups?.forEach { resultGroup ->
                resultGroup?.result?.filterNotNull()?.forEach { result ->
                    if (result.typeSpecific != null) {
                        when {
                            result.typeSpecific.onWeb != null -> {
                                webResults.add(result.toWebSearch())
                            }

                            result.typeSpecific.onRelatedSearches != null -> {
                                val relatedSearch =
                                    result.typeSpecific.onRelatedSearches?.relatedSearches
                                val entries = relatedSearch?.entries

                                relatedResults = entries?.mapNotNull { it.searchText }
                                    ?: emptyList()
                            }
                        }
                    }
                }
            }

            return@map NeevascopeResult(webSearches = webResults, relatedSearches = relatedResults)
        }
        .flowOn(dispatchers.io)
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private suspend fun performNeevaScopeQuery(query: String): SearchQuery.Data? {
        var searchResult: SearchQuery.Data? = null

        try {
            searchResult = apolloWrapper.performQuery(
                SearchQuery(query = query),
                userMustBeLoggedIn = false
            )?.data
        } catch (e: Exception) {
            // TODO: Show error states
            Log.e(TAG, "Caught exception while performing query. ", e)
        }

        return searchResult
    }

    suspend fun updateQuery(query: String) {
        _searchQuery.value = query
    }
}

fun SearchQuery.Result.toWebSearch(): NeevascopeWebResult {
    val web = this.typeSpecific?.onWeb?.web
    return NeevascopeWebResult(
        faviconURL = web?.favIconURL.toString(),
        displayURLHost = web?.structuredUrl?.hostname.toString(),
        displayURLPath = web?.structuredUrl?.paths,
        actionURL = Uri.parse(this.actionURL),
        title = this.title.toString(),
        snippet = this.snippet,
        publicationDate = web?.publicationDate
    )
}
