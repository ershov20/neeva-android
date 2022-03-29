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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.spaces.SpaceRow
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.suggestions.QuerySuggestionRow
import com.neeva.app.ui.layouts.GridLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.collapsingsection.collapsingSection
import com.neeva.app.ui.widgets.collapsingsection.collapsingThreeStateSection
import com.neeva.app.urlbar.URLBarModel

data class SuggestedSite(
    val site: Site,
    val overrideDrawableId: Int? = null
)

@Composable
fun ZeroQuery(
    urlBarModel: URLBarModel,
    faviconCache: FaviconCache,
    topContent: @Composable () -> Unit = {},
) {
    val browserWrapper = LocalBrowserWrapper.current
    val domainProvider = LocalEnvironment.current.domainProvider
    val historyManager = LocalEnvironment.current.historyManager
    val spaceStore = LocalEnvironment.current.spaceStore
    val sharedPreferencesModel = LocalEnvironment.current.sharedPreferencesModel

    val spaces: List<Space> by spaceStore.allSpacesFlow.collectAsState()

    val suggestedQueries by historyManager.suggestedQueries.collectAsState(emptyList())
    val suggestedSites by historyManager.suggestedSites.collectAsState(emptyList())

    val homeLabel = stringResource(id = R.string.home)

    val zeroQueryModel = remember { ZeroQueryModel(sharedPreferencesModel) }
    val isSuggestedSitesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SuggestedSitesState)
    val isSuggestedQueriesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SuggestedQueriesState)
    val isSpacesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SpacesState)

    val suggestedSitesPlusHome: List<SuggestedSite> = remember(suggestedSites) {
        suggestedSites
            .map { SuggestedSite(it) }
            .toMutableList()
            .apply {
                // The first suggested item should always send the user Home.
                add(
                    index = 0,
                    element = SuggestedSite(
                        site = Site(
                            siteURL = NeevaConstants.appURL,
                            title = homeLabel,
                            largestFavicon = null
                        ),
                        overrideDrawableId = R.drawable.ic_house
                    )
                )
            }
            .take(8)
    }

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

        if (suggestedQueries.isNotEmpty()) {
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
                items(suggestedQueries) { search ->
                    QuerySuggestionRow(
                        suggestion = search,
                        onLoadUrl = browserWrapper::loadUrl,
                        onEditUrl = { urlBarModel.replaceLocationBarText(search.query) }
                    )
                }
            }
        }

        if (spaces.isNotEmpty()) {
            collapsingSection(
                label = R.string.spaces,
                collapsingSectionState = isSpacesExpanded.value,
                onUpdateCollapsingSectionState = {
                    zeroQueryModel.advanceState(ZeroQueryPrefs.SpacesState)
                }
            ) {
                items(spaces.subList(0, minOf(3, spaces.size))) { space ->
                    SpaceRow(space = space, isCurrentUrlInSpace = null) {
                        browserWrapper.loadUrl(space.url())
                    }
                }
            }
        }
    }
}

fun Site.toSearchSuggest(): QueryRowSuggestion? {
    if (!siteURL.startsWith(NeevaConstants.appSearchURL)) return null
    val query = Uri.parse(this.siteURL).getQueryParameter("q") ?: return null

    return QueryRowSuggestion(
        url = Uri.parse(this.siteURL),
        query = query,
        drawableID = R.drawable.ic_baseline_history_24
    )
}
