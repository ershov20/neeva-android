package com.neeva.app.zeroQuery

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neeva.app.NeevaConstants
import com.neeva.app.NeevaConstants.appHost
import com.neeva.app.R
import com.neeva.app.spaces.SpaceRow
import com.neeva.app.storage.Site
import com.neeva.app.storage.Space
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.suggestions.QuerySuggestionRow
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.CollapsingState
import com.neeva.app.widgets.ComposableSingletonEntryPoint
import com.neeva.app.widgets.FaviconView
import com.neeva.app.widgets.collapsibleHeaderItem
import com.neeva.app.widgets.collapsibleHeaderItems
import dagger.hilt.EntryPoints
import kotlinx.coroutines.flow.map

// TODO(kobec): ask how to fix this
@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun ZeroQuery(
    urlBarModel: URLBarModel,
    topContent: @Composable (LazyItemScope.() -> Unit) = {},
) {
    val entryPoint = EntryPoints.get(
        LocalContext.current.applicationContext,
        ComposableSingletonEntryPoint::class.java
    )
    val historyManager = entryPoint.historyManager()
    val spaceStore = entryPoint.spaceStore()
    val domainProvider = entryPoint.domainProvider()

    val spaces: List<Space> by spaceStore.allSpacesFlow.collectAsState()

    /** Takes the top 3 suggestions for display to the user. */
    val suggestedQueries: List<QueryRowSuggestion> by historyManager.frequentSites
        .map { siteList ->
            siteList.mapNotNull { it.toSearchSuggest() }.take(3)
        }
        .collectAsState(emptyList())

    val suggestedSites: List<Site> by historyManager.frequentSites
        .map { sites ->
            // Assume that anything pointing at neeva.com should not be recommended to the user.
            // This includes search suggestions and Spaces, e.g.
            sites.filterNot {
                domainProvider.getRegisteredDomain(Uri.parse(it.siteURL)) == appHost
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
                LazyRow(modifier = Modifier.padding(vertical = 16.dp)) {
                    items(suggestedSites.subList(0, minOf(suggestedSites.size - 1, 8))) { site ->
                        val favicon = site.largestFavicon
                        val title = site.metadata?.title
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(onClickLabel = title) {
                                    urlBarModel.loadUrl(Uri.parse(site.siteURL))
                                }
                                .padding(horizontal = 16.dp)
                                .width(64.dp)
                        ) {
                            FaviconView(favicon = favicon, bordered = false, size = 48.dp)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = title ?: "",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    onEditUrl = { urlBarModel.replaceLocationBarText(search.query) }
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

fun Site.toSearchSuggest(): QueryRowSuggestion? {
    if (!siteURL.startsWith(NeevaConstants.appSearchURL)) return null
    val query = Uri.parse(this.siteURL).getQueryParameter("q") ?: return null

    return QueryRowSuggestion(
        url = Uri.parse(this.siteURL),
        query = query,
        drawableID = R.drawable.ic_baseline_history_24
    )
}
