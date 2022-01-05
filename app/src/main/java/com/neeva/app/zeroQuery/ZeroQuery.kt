package com.neeva.app.zeroQuery

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.NeevaConstants
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.R
import com.neeva.app.browsing.baseDomain
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.spaces.SpaceRow
import com.neeva.app.storage.Site
import com.neeva.app.storage.Space
import com.neeva.app.storage.SpaceStore
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.suggestions.QuerySuggestionRow
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.CollapsingState
import com.neeva.app.widgets.collapsibleHeaderItem
import com.neeva.app.widgets.collapsibleHeaderItems
import kotlinx.coroutines.flow.map
import java.util.*

@Composable
fun ZeroQuery(
    urlBarModel: URLBarModel,
    historyViewModel: HistoryViewModel,
    spaceStore: SpaceStore,
    topContent: @Composable() (LazyItemScope.() -> Unit) = {},
) {
    val spaces: List<Space> by spaceStore.allSpacesFlow.collectAsState()

    /** Takes the top 3 suggestions for display to the user. */
    val suggestedQueries: List<QueryRowSuggestion> by historyViewModel.frequentSites
        .map { siteList ->
            siteList.mapNotNull { it.toSearchSuggest() }.take(3)
        }
        .collectAsState(emptyList())

    val suggestedSites: List<Site> by historyViewModel.frequentSites
        .map { sites ->
            // Assume that anything pointing at https://www.neeva.com should not be recommended to
            // the user.  This includes search suggestions and Spaces, e.g.
            sites.filterNot {
                it.siteURL.contains(appURL)
            }
        }.collectAsState(emptyList())

    val searchesLabel = stringResource(id = R.string.searches)
    val spacesLabel = stringResource(id = R.string.spaces)
    val suggestedSitesLabel = stringResource(id = R.string.suggested_sites)

    LazyColumn {
        item {
            topContent()
        }

        if (suggestedSites.isNotEmpty()) {
            collapsibleHeaderItem(
                label = suggestedSitesLabel,
                startingState = CollapsingState.SHOW_COMPACT,
            ) {
                LazyRow(modifier = Modifier.padding(16.dp)) {
                    items(suggestedSites.subList(0, minOf(suggestedSites.size - 1, 8))) { site ->
                        val bitmap = site.largestFavicon?.toBitmap()
                        val siteName = Uri.parse(site.siteURL).baseDomain().toString()
                            .split(".").first()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { urlBarModel.loadUrl(Uri.parse(site.siteURL)) }
                        ) {
                            val imageModifier = Modifier
                                .size(36.dp)
                                .padding(2.dp)
                            if (bitmap == null) {
                                Image(
                                    painter = painterResource(id = R.drawable.globe),
                                    contentDescription = null,
                                    modifier = imageModifier,
                                    contentScale = ContentScale.FillBounds,
                                )
                            } else {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = imageModifier,
                                    contentScale = ContentScale.FillBounds,
                                )
                            }
                            Text(
                                text = siteName,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        if (suggestedQueries.isNotEmpty()) {
            collapsibleHeaderItems(
                label = searchesLabel,
                startingState = CollapsingState.SHOW_COMPACT,
                items = suggestedQueries,
            ) { search ->
                QuerySuggestionRow(
                    suggestion = search,
                    onLoadUrl = urlBarModel::loadUrl,
                    onEditUrl = null
                )
            }
        }

        if (spaces.isNotEmpty()) {
            collapsibleHeaderItems(
                label = spacesLabel,
                startingState = CollapsingState.SHOW_COMPACT,
                items = spaces.subList(0, minOf(3, spaces.size)),
            ) { space ->
                SpaceRow(space = space) {
                    urlBarModel.loadUrl(space.url)
                }
            }
        }

    }
}

fun Site.toSearchSuggest() : QueryRowSuggestion? {
    if (!siteURL.startsWith(NeevaConstants.appSearchURL)) return null
    val query = Uri.parse(this.siteURL).getQueryParameter("q") ?: return null

    return QueryRowSuggestion(
        url = Uri.parse(this.siteURL),
        query =  query,
        drawableID = R.drawable.ic_baseline_history_24
    )
}
