package com.neeva.app.zeroQuery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.browsing.urlbar.URLBarModel
import com.neeva.app.history.DefaultSuggestions
import com.neeva.app.spaces.SpaceRow
import com.neeva.app.spaces.SpaceRowData
import com.neeva.app.spaces.getThumbnailAsync
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.suggestions.QuerySuggestionRow
import com.neeva.app.ui.layouts.GridLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.collapsingsection.collapsingSection
import com.neeva.app.ui.widgets.collapsingsection.collapsingThreeStateSection

data class SuggestedSite(
    val site: Site,
    val overrideDrawableId: Int? = null
)

@Composable
fun ZeroQuery(
    urlBarModel: URLBarModel,
    faviconCache: FaviconCache,
    neevaConstants: NeevaConstants,
    topContent: @Composable () -> Unit = {},
) {
    val browserWrapper = LocalBrowserWrapper.current
    val domainProvider = LocalEnvironment.current.domainProvider
    val historyManager = LocalEnvironment.current.historyManager
    val appNavModel = LocalAppNavModel.current
    val spaceStore = LocalEnvironment.current.spaceStore
    val sharedPreferencesModel = LocalEnvironment.current.sharedPreferencesModel

    val spaces: List<Space> by spaceStore.allSpacesFlow.collectAsState()

    val suggestedQueries by historyManager.suggestedQueries.collectAsState(emptyList())
    val suggestedSites by historyManager.suggestedSites.collectAsState(emptyList())

    val homeLabel = stringResource(id = R.string.home)

    val zeroQueryModel = remember { ZeroQueryModel(sharedPreferencesModel) }
    val isSuggestedSitesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SuggestedSitesState)
    val isSuggestedQueriesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SuggestedQueriesState)
    val isCommunitySpacesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.CommunitySpacesState)
    val isSpacesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SpacesState)

    val defaultSuggestions = DefaultSuggestions(neevaConstants = neevaConstants)

    val suggestedSearchesWithDefaults: List<QueryRowSuggestion> = remember(suggestedQueries) {
        val updatedList = suggestedQueries.toMutableList()
        defaultSuggestions.DEFAULT_SEARCH_SUGGESTIONS.forEach { suggestion ->
            if (updatedList.size < 3 && updatedList.none { it.query == suggestion.query }) {
                updatedList.add(suggestion)
            }
        }
        return@remember updatedList
    }

    val suggestedSitesPlusHome: List<SuggestedSite> = remember(suggestedSites) {
        val updatedList: MutableList<Site> = suggestedSites.toMutableList()
        val domainList = updatedList
            .map { domainProvider.getRegisteredDomain(Uri.parse(it.siteURL)) }
        defaultSuggestions.DEFAULT_SITE_SUGGESTIONS.forEach { site ->
            val siteDomain = domainProvider.getRegisteredDomain(Uri.parse(site.siteURL))
            if (updatedList.size < 7 && !domainList.contains(siteDomain)) {
                updatedList.add(site)
            }
        }
        updatedList
            .map { SuggestedSite(it) }
            .toMutableList()
            .apply {
                // The first suggested item should always send the user Home.
                add(
                    index = 0,
                    element = SuggestedSite(
                        site = Site(
                            siteURL = neevaConstants.appURL,
                            title = homeLabel,
                            largestFavicon = null
                        ),
                        overrideDrawableId = R.drawable.ic_house
                    )
                )
            }
            .take(8)
    }

    val communitySpaces: List<SpaceRowData>
        by spaceStore.spacesFromCommunityFlow.collectAsState(emptyList())

    LazyColumn(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        item {
            topContent()
        }

        collapsingThreeStateSection(
            label = R.string.suggested_sites,
            collapsingSectionState = isSuggestedSitesExpanded.value,
            onUpdateCollapsingSectionState = {
                zeroQueryModel.advanceState(ZeroQueryPrefs.SuggestedSitesState)
            },
            expandedContent = {
                // Draw everything as a 4x2 grid with the width evenly divided.
                GridLayout(4, suggestedSitesPlusHome) { suggestedSite ->
                    ZeroQuerySuggestedSite(
                        suggestedSite = suggestedSite,
                        faviconCache = faviconCache,
                        domainProvider = domainProvider,
                        onClick = browserWrapper::loadUrl
                    )
                }
            },
            compactContent = {
                // Draw everything as a scrollable Row in the main list.  Keep all of the icons the
                // same width (64.dp + large padding + large padding).
                Row(
                    modifier = Modifier
                        .padding(vertical = Dimensions.PADDING_LARGE)
                        .horizontalScroll(rememberScrollState())
                ) {
                    suggestedSitesPlusHome.forEach { suggestedSite ->
                        Box(modifier = Modifier.width(64.dp + Dimensions.PADDING_LARGE * 2)) {
                            ZeroQuerySuggestedSite(
                                suggestedSite = suggestedSite,
                                faviconCache = faviconCache,
                                domainProvider = domainProvider,
                                onClick = browserWrapper::loadUrl
                            )
                        }
                    }
                }
            }
        )

        if (suggestedSearchesWithDefaults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
            }

            collapsingSection(
                label = R.string.searches,
                collapsingSectionState = isSuggestedQueriesExpanded.value,
                onUpdateCollapsingSectionState = {
                    zeroQueryModel.advanceState(ZeroQueryPrefs.SuggestedQueriesState)
                }
            ) {
                items(suggestedSearchesWithDefaults) { search ->
                    QuerySuggestionRow(
                        suggestion = search,
                        onLoadUrl = browserWrapper::loadUrl,
                        onEditUrl = { urlBarModel.replaceLocationBarText(search.query) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
        }

        collapsingSection(
            label = R.string.community_spaces,
            collapsingSectionState = isCommunitySpacesExpanded.value,
            onUpdateCollapsingSectionState = {
                zeroQueryModel.advanceState(ZeroQueryPrefs.CommunitySpacesState)
            }
        ) {
            items(communitySpaces.take(5), key = { it.id }) {
                val thumbnail: ImageBitmap? by getThumbnailAsync(uri = it.thumbnail)
                SpaceRow(
                    spaceName = it.name,
                    isSpacePublic = it.isPublic,
                    thumbnail = thumbnail,
                    isCurrentUrlInSpace = null
                ) {
                    appNavModel.openUrl(it.url())
                    appNavModel.showBrowser()
                }
            }
        }

        if (spaces.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
            }

            collapsingSection(
                label = R.string.spaces,
                collapsingSectionState = isSpacesExpanded.value,
                onUpdateCollapsingSectionState = {
                    zeroQueryModel.advanceState(ZeroQueryPrefs.SpacesState)
                }
            ) {
                items(spaces.subList(0, minOf(3, spaces.size))) { space ->
                    SpaceRow(space = space, isCurrentUrlInSpace = null) {
                        browserWrapper.loadUrl(space.url(neevaConstants))
                    }
                }
            }
        }
    }
}

fun Site.toSearchSuggest(neevaConstants: NeevaConstants): QueryRowSuggestion? {
    if (!siteURL.startsWith(neevaConstants.appSearchURL)) return null
    val query = Uri.parse(this.siteURL).getQueryParameter("q") ?: return null

    return QueryRowSuggestion(
        url = Uri.parse(this.siteURL),
        query = query,
        drawableID = R.drawable.ic_baseline_history_24
    )
}

fun String.toSearchSuggest(neevaConstants: NeevaConstants): QueryRowSuggestion = QueryRowSuggestion(
    url = this.toSearchUri(neevaConstants),
    query = this,
    drawableID = R.drawable.ic_baseline_history_24
)
