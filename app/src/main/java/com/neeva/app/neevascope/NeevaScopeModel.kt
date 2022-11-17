// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neeva.app.CheatsheetInfoQuery
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.SearchQuery
import com.neeva.app.apollo.ApolloWrapper
import com.neeva.app.browsing.isNeevaUri
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.ui.PopupModel
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

data class NeevaScopeResult(
    val webSearches: List<NeevaScopeWebResult> = emptyList(),
    val relatedSearches: List<String> = emptyList(),
    val redditDiscussions: List<NeevaScopeDiscussion> = emptyList(),
    val memorizedSearches: List<String> = emptyList(),
    val recipe: NeevaScopeRecipe?
)

enum class NeevaScopePromoType {
    TRY_NEEVASCOPE,
    TRY_UGC
}

class NeevaScopeModel(
    private val apolloWrapper: ApolloWrapper,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    appContext: Context,
    val bloomFilterManager: BloomFilterManager,
    private val neevaConstants: NeevaConstants,
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val settingsDataModel: SettingsDataModel
) {
    companion object {
        private const val MEMORIZED_QUERY_COUNT_THRESHOLD = 5
        private const val DISCUSSION_COUNT_THRESHOLD = 5
        val TAG = NeevaScopeModel::class.simpleName
    }

    data class RedditPromoState(
        var showRedditTooltip: Boolean,
        var showRedditDot: Boolean
    ) {
        companion object {
            val missFilter = RedditPromoState(showRedditTooltip = false, showRedditDot = false)
            val hitFilter = RedditPromoState(showRedditTooltip = true, showRedditDot = false)
        }
    }

    enum class PromoTransition {
        DISMISS_TOOLTIP, DISMISS_DOT
    }

    // region NeevaScope content
    private val _searchQuery = MutableStateFlow<NeevaScopeSearchQuery?>(value = null)
    val searchQuery: StateFlow<NeevaScopeSearchQuery?> get() = _searchQuery
    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    val searchFlow: StateFlow<NeevaScopeResult?> = searchQuery
        .filterNotNull()
        .map { search ->
            _isLoading.value = true

            // Neeva search
            val searchResultsData = performNeevaScopeQuery(query = search.query)

            // Neeva cheatsheet
            val cheatsheetInfoData = performCheatsheetInfoQuery(search.query, search.title)

            _isLoading.value = false

            if (
                searchResultsData?.search?.resultGroup?.isEmpty() == true &&
                cheatsheetInfoData?.getCheatsheetInfo == null
            ) {
                return@map null
            }

            return@map updateNeevaScopeResult(searchResultsData, cheatsheetInfoData, appContext)
        }
        .flowOn(dispatchers.io)
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    // endregion

    // region NeevaScope promo
    private val _urlFlow = MutableStateFlow<Uri>(value = Uri.EMPTY)
    val urlFlow: StateFlow<Uri> get() = _urlFlow

    private val _promoCache: MutableMap<Uri, RedditPromoState> = mutableMapOf()
    val promoCache: Map<Uri, RedditPromoState> get() = _promoCache

    val showRedditDot: Boolean get() = promoCache[urlFlow.value]?.showRedditDot ?: false

    val neevaScopeTooltipTypeFlow: StateFlow<NeevaScopePromoType?> = urlFlow
        .map {
            updateNeevaScopePromoType(it)
        }
        .flowOn(dispatchers.io)
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    // endregion

    fun updateQuery(query: String, title: String) {
        _searchQuery.value = NeevaScopeSearchQuery(query = query, title = title)
    }

    private suspend fun performNeevaScopeQuery(query: String): SearchQuery.Data? {
        var searchResult: SearchQuery.Data? = null

        try {
            searchResult = apolloWrapper.performQuery(
                SearchQuery(query = query),
                userMustBeLoggedIn = false
            ).response?.data
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
                userMustBeLoggedIn = false
            ).response?.data
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
        val recipe = cheatsheetInfoData?.getCheatsheetInfo?.Recipe?.toNeevaScopeRecipe()

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
            memorizedSearches = memorizedResults.take(MEMORIZED_QUERY_COUNT_THRESHOLD),
            recipe = recipe
        )
    }

    fun updateUrl(url: Uri) {
        _urlFlow.value = url
    }

    /**
     * Update NeevaScope promo type of current website
     *
     * @param uri the URI to get the NeevaScope promo state
     * @return the [NeevaScopePromoType] TRY_UGC if the current website hits the bloom filter,
     * TRY_NEEVASCOPE if it does not hit the filter
     */
    fun updateNeevaScopePromoType(uri: Uri): NeevaScopePromoType? {
        if (uri == Uri.EMPTY || uri.isNeevaUri(neevaConstants)) return null

        updateRedditPromoState(uri)

        if (promoCache[uri]?.showRedditTooltip == true) {
            return NeevaScopePromoType.TRY_UGC
        } else {
            return NeevaScopePromoType.TRY_NEEVASCOPE
        }
    }

    /**
     * Check if current website hits the bloom filter and store in the promoCache
     *
     * @param uri the URI to look for in the bloom filter
     */
    private fun updateRedditPromoState(uri: Uri) {
        val canonUrl = CanonicalUrl().canonicalizeUrl(uri, true)
        if (canonUrl == Uri.EMPTY) {
            _promoCache[uri] = RedditPromoState.missFilter
            return
        }

        val filterResult: Boolean? = bloomFilterManager.contains(canonUrl.toString())
        when (filterResult) {
            // The filter is still loading.
            null -> return

            true -> _promoCache[uri] = RedditPromoState.hitFilter
            false -> _promoCache[uri] = RedditPromoState.missFilter
        }
    }

    /**
     * Perform the promo transition and update the promoCache
     *
     * @param transition The PromoTransition to update the promo state of current uri
     */
    fun performRedditPromoTransition(transition: PromoTransition) {
        val state = promoCache[urlFlow.value] ?: RedditPromoState.missFilter

        when (transition) {
            PromoTransition.DISMISS_TOOLTIP -> {
                // If uri hits bloom filter, blue dot will appear after dismissing tooltip
                if (state.showRedditTooltip) {
                    state.showRedditDot = true
                } else {
                    SharedPrefFolder.App.NeevaScopeTooltipCount.set(
                        sharedPreferencesModel,
                        SharedPrefFolder.App.NeevaScopeTooltipCount.get(sharedPreferencesModel) - 1
                    )
                }

                if (SharedPrefFolder.App.NeevaScopeTooltipCount.get(sharedPreferencesModel) <= 0) {
                    state.showRedditTooltip = false
                }
            }
            PromoTransition.DISMISS_DOT -> {
                state.showRedditDot = false
                SharedPrefFolder.App.NeevaScopeTooltipCount.set(
                    sharedPreferencesModel,
                    SharedPrefFolder.App.NeevaScopeTooltipCount.get(sharedPreferencesModel) - 1
                )

                if (SharedPrefFolder.App.NeevaScopeTooltipCount.get(sharedPreferencesModel) <= 0) {
                    state.showRedditTooltip = false
                }
            }
        }

        if (SharedPrefFolder.App.NeevaScopeTooltipCount.get(sharedPreferencesModel) <= 0) {
            settingsDataModel.setToggleState(SettingsToggle.ENABLE_NEEVASCOPE_TOOLTIP, false)
        }

        _promoCache[urlFlow.value] = state
    }

    fun showNeevaScopeResult(
        currentUrl: Uri,
        currentTitle: String,
        isOnNeevaSearch: Boolean,
        faviconCache: FaviconCache,
        popupModel: PopupModel
    ) {
        // If user clicks on the X button instead of "Let' try it" button on non-Neeva page,
        // do not update the SeenNeevaScopeIntro immediately.
        if (settingsDataModel.getSettingsToggleValue(SettingsToggle.ENABLE_NEEVASCOPE_TOOLTIP)) {
            SharedPrefFolder.App.SeenNeevaScopeIntro.set(sharedPreferencesModel, true)
        }

        if (currentUrl != Uri.EMPTY) {
            updateQuery(currentUrl.toString(), currentTitle)
        }

        val seenNeevaScopeIntroFlow =
            SharedPrefFolder.App.SeenNeevaScopeIntro.getFlow(sharedPreferencesModel)

        popupModel.showBottomSheet(
            hasHalfwayState = !isOnNeevaSearch && seenNeevaScopeIntroFlow.value
        ) { onDismiss ->
            val isLoading by isLoading.collectAsState()
            val seenNeevaScopeIntro by seenNeevaScopeIntroFlow.collectAsState()

            when {
                isOnNeevaSearch -> {
                    NeevaScopeInfoScreen(
                        buttonTextId = R.string.neevascope_got_it,
                        tapButton = {
                            if (!seenNeevaScopeIntro) {
                                settingsDataModel
                                    .setToggleState(SettingsToggle.ENABLE_NEEVASCOPE_TOOLTIP, true)
                            }
                            onDismiss()
                        },
                        dismissSheet = onDismiss
                    )
                }

                !seenNeevaScopeIntro -> {
                    NeevaScopeInfoScreen(
                        buttonTextId = R.string.neevascope_lets_try_it,
                        tapButton = {
                            SharedPrefFolder.App.SeenNeevaScopeIntro.set(
                                sharedPreferencesModel,
                                true
                            )
                        },
                        dismissSheet = {
                            settingsDataModel
                                .setToggleState(SettingsToggle.ENABLE_NEEVASCOPE_TOOLTIP, true)
                            onDismiss()
                        }
                    )
                }

                isLoading -> {
                    NeevaScopeLoadingScreen()
                }

                else -> {
                    settingsDataModel
                        .setToggleState(SettingsToggle.ENABLE_NEEVASCOPE_TOOLTIP, false)

                    NeevaScopeResultScreen(
                        neevascopeModel = this,
                        onDismiss = onDismiss,
                        faviconCache = faviconCache,
                        currentUrl = currentUrl
                    )
                }
            }

            if (
                settingsDataModel.getSettingsToggleValue(SettingsToggle.ENABLE_NEEVASCOPE_TOOLTIP)
            ) {
                if (promoCache[currentUrl]?.showRedditDot == true) {
                    performRedditPromoTransition(PromoTransition.DISMISS_DOT)
                }
            }
        }
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
        url = if (this.url == null) Uri.EMPTY else Uri.parse(this.url),
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
    val date = try {
        ZonedDateTime.parse(this, formatter).toLocalDate()
    } catch (e: Exception) {
        return ""
    }

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

fun CheatsheetInfoQuery.Recipe.toNeevaScopeRecipe(): NeevaScopeRecipe {
    return NeevaScopeRecipe(
        title = this.title ?: "",
        imageURL = this.imageURL ?: "",
        totalTime = this.totalTime,
        prepTime = this.prepTime,
        yield = this.yield,
        ingredients = this.ingredients?.mapNotNull { ingredient ->
            ingredient.text
        },
        instructions = this.instructions?.mapNotNull { instruction ->
            instruction.text
        },
        recipeRating = this.recipeRating?.let { recipeRating ->
            RecipeRating(
                maxStars = recipeRating.maxStars ?: 0.0,
                recipeStars = recipeRating.recipeStars ?: 0.0,
                numReviews = recipeRating.numReviews ?: 0
            )
        },
        reviews = this.reviews?.map { review ->
            RecipeReview(
                reviewerName = review.reviewerName ?: "",
                body = review.body ?: "",
                rating = review.rating.let { rating ->
                    ReviewRating(
                        maxStars = rating?.maxStars ?: 0.0,
                        actualStars = rating?.actualStars ?: 0.0
                    )
                }
            )
        }
    )
}
