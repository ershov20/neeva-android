package com.neeva.app.neevascope

import android.content.Context
import android.net.Uri
import android.util.Log
import com.neeva.app.CheatsheetInfoQuery
import com.neeva.app.Dispatchers
import com.neeva.app.R
import com.neeva.app.SearchQuery
import com.neeva.app.apollo.ApolloWrapper
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NeevaScopeSearchQuery(
    val query: String,
    val title: String
)

data class NeevaScopeWebResult(
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

data class NeevaScopeDiscussion(
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

data class NeevaScopeResult(
    val webSearches: List<NeevaScopeWebResult> = emptyList(),
    val relatedSearches: List<String> = emptyList(),
    val redditDiscussions: List<NeevaScopeDiscussion> = emptyList(),
    val memorizedSearches: List<String> = emptyList()
)

class NeevaScopeModel(
    private val apolloWrapper: ApolloWrapper,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    appContext: Context
) {
    companion object {
        private const val MEMORIZED_QUERY_COUNT_THRESHOLD = 5
        private const val DISCUSSION_COUNT_THRESHOLD = 5
        val TAG = NeevaScopeModel::class.simpleName
    }

    private val _searchQuery = MutableStateFlow<NeevaScopeSearchQuery?>(value = null)
    val searchQuery: StateFlow<NeevaScopeSearchQuery?> get() = _searchQuery
    val searchFlow: StateFlow<NeevaScopeResult?> = searchQuery
        .filterNotNull()
        .map { search ->
            // Neeva search
            val searchResultsData = performNeevaScopeQuery(query = search.query)

            // Neeva cheatsheet
            val cheatsheetInfoData = performCheatsheetInfoQuery(search.query, search.title)

            return@map updateNeevaScopeResult(searchResultsData, cheatsheetInfoData, appContext)
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

    suspend fun updateNeevaScopeResult(
        searchResultsData: SearchQuery.Data?,
        cheatsheetInfoData: CheatsheetInfoQuery.Data?,
        appContext: Context
    ): NeevaScopeResult {
        val searchResultGroups = searchResultsData?.search?.resultGroup
        val webResults: MutableList<NeevaScopeWebResult> = mutableListOf()
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

        val discussions: MutableList<NeevaScopeDiscussion> = mutableListOf()
        val memorizedResults =
            cheatsheetInfoData?.getCheatsheetInfo?.MemorizedQuery ?: emptyList()

        cheatsheetInfoData?.getCheatsheetInfo?.BacklinkURL
            ?.mapNotNull { backlinkURL ->
                backlinkURL
                    .takeIf { it.Domain == "www.reddit.com" }
                    ?.toRedditDiscussion(appContext)
                    ?.let { discussion ->
                        discussions.add(discussion)
                    }
            }

        return NeevaScopeResult(
            webSearches = webResults,
            relatedSearches = relatedResults,
            redditDiscussions = discussions.take(DISCUSSION_COUNT_THRESHOLD),
            memorizedSearches = memorizedResults.take(MEMORIZED_QUERY_COUNT_THRESHOLD)
        )
    }

    fun updateQuery(query: String, title: String) {
        _searchQuery.value = NeevaScopeSearchQuery(query = query, title = title)
    }
}

fun SearchQuery.Result.toWebSearch(): NeevaScopeWebResult {
    val web = this.typeSpecific?.onWeb?.web
    return NeevaScopeWebResult(
        faviconURL = web?.favIconURL.toString(),
        displayURLHost = web?.structuredUrl?.hostname.toString(),
        displayURLPath = web?.structuredUrl?.paths,
        actionURL = Uri.parse(this.actionURL),
        title = this.title.toString(),
        snippet = this.snippet,
        publicationDate = web?.publicationDate
    )
}

fun CheatsheetInfoQuery.BacklinkURL.toRedditDiscussion(appContext: Context): NeevaScopeDiscussion? {
    val content = this.Forum?.toDiscussionContent()
    val slash = this.URL?.toDiscussionSlash()

    return content?.let {
        NeevaScopeDiscussion(
            title = this.Title.toString(),
            content = it,
            url = Uri.parse(this.URL),
            slash = slash.toString(),
            upvotes = this.Forum?.score,
            numComments = this.Forum?.numComments,
            interval = this.Forum?.date?.toInterval(appContext)
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
        ?.let { this.substring(it).drop(1) }
        ?.toString()
}

fun String.toInterval(appContext: Context): String {
    if (this.isEmpty()) return ""

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z z")
    val date = this
        .let {
            ZonedDateTime.parse(it, formatter)
        }.toLocalDate()

    return when {
        Period.between(date, LocalDate.now()).years > 0 -> {
            appContext.resources.getQuantityString(
                R.plurals.discussion_interval_year,
                Period.between(date, LocalDate.now()).years,
                Period.between(date, LocalDate.now()).years
            )
        }

        Period.between(date, LocalDate.now()).months > 0 -> {
            appContext.resources.getQuantityString(
                R.plurals.discussion_interval_month,
                Period.between(date, LocalDate.now()).months,
                Period.between(date, LocalDate.now()).months
            )
        }

        else -> {
            appContext.resources.getQuantityString(
                R.plurals.discussion_interval_day,
                Period.between(date, LocalDate.now()).days,
                Period.between(date, LocalDate.now()).days
            )
        }
    }
}
