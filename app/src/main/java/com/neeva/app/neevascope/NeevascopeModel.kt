package com.neeva.app.neevascope

import android.net.Uri
import android.util.Log
import com.neeva.app.CheatsheetInfoQuery
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

data class NeevascopeSearchQuery(
    val query: String,
    val title: String
)

data class NeevascopeWebResult(
    val faviconURL: String,
    val displayURLHost: String,
    val displayURLPath: List<String>?,
    val actionURL: Uri,
    val title: String,
    val snippet: String? = null,
    val publicationDate: String? = null
)

data class DiscussionComment(
    val body: String,
    val url: Uri? = null,
    val upvotes: Int? = null
)

data class DiscussionContent(
    val body: String,
    val comments: List<DiscussionComment>
)

data class NeevascopeDiscussion(
    // Required properties
    val title: String,
    val content: DiscussionContent,
    val url: Uri,
    val slash: String,

    // Optionally displayed properties
    val upvotes: Int? = null,
    val numComments: Int? = null,
    val interval: String? = null,
)

data class NeevascopeResult(
    val webSearches: List<NeevascopeWebResult> = emptyList(),
    val relatedSearches: List<String> = emptyList(),
    val redditDiscussions: List<NeevascopeDiscussion> = emptyList(),
    val memorizedSearches: List<String> = emptyList()
)

class NeevascopeModel(
    private val apolloWrapper: ApolloWrapper,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers
) {
    companion object {
        val TAG = NeevascopeModel::class.simpleName
    }

    private val _searchQuery = MutableStateFlow<NeevascopeSearchQuery?>(value = null)
    val searchQuery: StateFlow<NeevascopeSearchQuery?> get() = _searchQuery
    val searchFlow: StateFlow<NeevascopeResult?> = searchQuery
        .filterNotNull()
        .map { search ->
            // Neeva search
            val searchResultsData = performNeevaScopeQuery(query = search.query)

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
                                    result.typeSpecific.onRelatedSearches.relatedSearches
                                val entries = relatedSearch?.entries

                                relatedResults = entries?.mapNotNull { it.searchText }
                                    ?: emptyList()
                            }
                        }
                    }
                }
            }

            // Neeva cheatsheet
            val cheatsheetInfoData = performCheatsheetInfoQuery(search.query, search.title)
            val discussions: MutableList<NeevascopeDiscussion> = mutableListOf()
            val memorizedResults =
                cheatsheetInfoData?.getCheatsheetInfo?.MemorizedQuery ?: emptyList()

            cheatsheetInfoData?.getCheatsheetInfo?.BacklinkURL
                ?.mapNotNull { backlinkURL ->
                    backlinkURL.toRedditDiscussion()?.let { discussion ->
                        discussions.add(discussion)
                    }
                }

            return@map NeevascopeResult(
                webSearches = webResults,
                relatedSearches = relatedResults,
                redditDiscussions = discussions,
                memorizedSearches = memorizedResults
            )
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
            // TODO(https://github.com/neevaco/neeva-android/issues/826): Show error states
            Log.e(TAG, "Caught exception while performing query. ", e)
        }

        return searchResult
    }

    private suspend fun performCheatsheetInfoQuery(
        input: String,
        title: String
    ): CheatsheetInfoQuery.Data? {
        var cheatsheetInfo: CheatsheetInfoQuery.Data? = null

        try {
            cheatsheetInfo = apolloWrapper.performQuery(
                CheatsheetInfoQuery(input = input, title = title),
                userMustBeLoggedIn = true
            )?.data
        } catch (e: Exception) {
            // TODO(https://github.com/neevaco/neeva-android/issues/826): Show error states
            Log.e(TAG, "Caught exception while performing query. ", e)
        }

        return cheatsheetInfo
    }

    fun updateQuery(query: String, title: String) {
        _searchQuery.value = NeevascopeSearchQuery(query = query, title = title)
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

fun CheatsheetInfoQuery.BacklinkURL.toRedditDiscussion(): NeevascopeDiscussion? {
    val content = this.Forum?.toDiscussionContent()
    val slash = this.URL?.toDiscussionSlash()

    return content?.let {
        NeevascopeDiscussion(
            title = this.Title.toString(),
            content = it,
            url = Uri.parse(this.URL),
            slash = slash.toString(),
            numComments = this.Forum?.numComments
        )
    }
}

fun CheatsheetInfoQuery.Forum.toDiscussionContent(): DiscussionContent {
    val body = this.body.toString()
    val commentContents = this.comments
        ?.map { it.toDiscussionComment() }
        ?: emptyList()

    return DiscussionContent(body = body, comments = commentContents)
}

fun CheatsheetInfoQuery.Comment.toDiscussionComment(): DiscussionComment {
    return DiscussionComment(
        body = this.body.toString(),
        url = Uri.parse(this.url),
        upvotes = this.score
    )
}

fun String.toDiscussionSlash(): String? {
    // extract /r in URL
    val matchResult = Regex("/r/([a-zA-Z0-9_]{1,21}+)").find(this)
    return matchResult?.range
        ?.let { this.substring(it) }
        ?.toString()
}
