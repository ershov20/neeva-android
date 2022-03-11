package com.neeva.app.zeroQuery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.neeva.app.suggestions.toUserVisibleString
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.FaviconView
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
            val faviconBitmap by faviconCache.getFaviconAsync(Uri.parse(suggestedSite.site.siteURL))
            val title = suggestedSite.site.title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClickLabel = title) {
                        browserWrapper.loadUrl(Uri.parse(suggestedSite.site.siteURL))
                    }
                    .padding(horizontal = 16.dp)
                    .width(64.dp)
            ) {
                FaviconView(
                    bitmap = faviconBitmap,
                    imageOverride = suggestedSite.iconOverride,
                    bordered = false,
                    size = 48.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = suggestedSite.site.toUserVisibleString(domainProvider),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
