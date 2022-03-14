package com.neeva.app.zeroQuery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.collapsibleSection

data class SuggestedSite(
    val site: Site,
    val iconOverride: ImageVector? = null
)

@Composable
fun ZeroQuery(
    urlBarModel: URLBarModel,
    faviconCache: FaviconCache,
    topContent: @Composable (LazyItemScope.(modifier: Modifier) -> Unit) = {},
) {
    val browserWrapper = LocalBrowserWrapper.current
    val domainProvider = LocalEnvironment.current.domainProvider
    val historyManager = LocalEnvironment.current.historyManager
    val spaceStore = LocalEnvironment.current.spaceStore

    val spaces: List<Space> by spaceStore.allSpacesFlow.collectAsState()

    val suggestedQueries by historyManager.suggestedQueries.collectAsState(emptyList())
    val suggestedSites by historyManager.suggestedSites.collectAsState(emptyList())

    val homeLabel = stringResource(id = R.string.home)

    val isSuggestedSitesExpanded = remember { mutableStateOf(true) }
    val isSuggestedQueriesExpanded = remember { mutableStateOf(true) }
    val isSpacesExpanded = remember { mutableStateOf(true) }

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
                        iconOverride = Icons.Default.Home
                    )
                )
            }
            .take(8)
    }

    LazyColumn(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        item {
            topContent(Modifier)
        }

        collapsibleSection(
            label = R.string.suggested_sites,
            displayedItems = suggestedSitesPlusHome,
            isExpanded = isSuggestedSitesExpanded,
            isDisplayedAsRow = true
        ) { suggestedSite ->
            // Force the size of the icon be 64.dp plus the padding on its sides.
            Box(modifier = Modifier.width(64.dp + (Dimensions.PADDING_LARGE * 2))) {
                ZeroQuerySuggestedSite(
                    suggestedSite = suggestedSite,
                    faviconCache = faviconCache,
                    domainProvider = domainProvider,
                    onClick = {
                        browserWrapper.loadUrl(Uri.parse(suggestedSite.site.siteURL))
                    }
                )
            }
        }

        if (suggestedQueries.isNotEmpty()) {
            collapsibleSection(
                label = R.string.searches,
                displayedItems = suggestedQueries,
                isExpanded = isSuggestedQueriesExpanded
            ) { search ->
                QuerySuggestionRow(
                    suggestion = search,
                    onLoadUrl = browserWrapper::loadUrl,
                    onEditUrl = { urlBarModel.replaceLocationBarText(search.query) }
                )
            }
        }

        if (spaces.isNotEmpty()) {
            collapsibleSection(
                label = R.string.spaces,
                displayedItems = spaces.subList(0, minOf(3, spaces.size)),
                isExpanded = isSpacesExpanded
            ) { space ->
                SpaceRow(space = space) {
                    browserWrapper.loadUrl(space.url)
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
