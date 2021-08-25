package com.neeva.app.zeroQuery

import android.graphics.Bitmap
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
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.neeva.app.appURL
import com.neeva.app.spaces.SpaceRow
import com.neeva.app.storage.*
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.web.WebLayerModel
import com.neeva.app.web.baseDomain
import com.neeva.app.widgets.CollapsingState
import com.neeva.app.widgets.FaviconView
import com.neeva.app.widgets.collapsibleHeaderItem
import com.neeva.app.widgets.collapsibleHeaderItems
import java.util.*

@Composable
fun ZeroQuery(
    webLayerModel: WebLayerModel,
    sitesViewModel: SitesViewModel,
    topContent: @Composable() (LazyItemScope.() -> Unit) = {},
) {
    val spaces: List<Space> by SpaceStore.shared.allSpacesFlow.asLiveData().observeAsState(emptyList())
    val suggestedQueries: List<QueryRowSuggestion> by sitesViewModel.frequentSites.map { siteList ->
        siteList.mapNotNull { it.toSearchSuggest() }.take(3)
    }.observeAsState(emptyList())
    val suggestedSites: List<Site> by sitesViewModel.frequentSites.observeAsState(emptyList())

    LazyColumn() {
        item {
            topContent()
        }

        collapsibleHeaderItem(
            label = "Suggested Sites",
            startingState = CollapsingState.SHOW_COMPACT,
        ) {
            LazyRow(modifier = Modifier.padding(16.dp)) {
                items(suggestedSites.filterNot { it.siteURL.contains(appURL) }
                    .subList(0, minOf(suggestedSites.size, 8))) { site ->
                    val bitmap = site.largestFavicon?.toBitmap() ?: Favicon.defaultFavicon
                    val siteName = Uri.parse(site.siteURL).baseDomain().toString()
                        .split(".").first()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp).clickable {
                            webLayerModel.loadUrl(Uri.parse(site.siteURL))
                        }
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Suggested Site",
                            modifier = Modifier
                                .size(32.dp)
                                .padding(2.dp),
                            contentScale = ContentScale.FillBounds,
                        )
                        Text(
                            text = siteName,
                            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        collapsibleHeaderItems(
            label = "Suggested Searches",
            startingState = CollapsingState.SHOW_COMPACT,
            items = suggestedQueries,
        ) { search ->
            QueryRowSuggestion(suggestion = search, onLoadUrl = webLayerModel::loadUrl)
        }

        collapsibleHeaderItems(
            label = "Suggested Spaces",
            startingState = CollapsingState.SHOW_COMPACT,
            items = spaces.subList(0, minOf(3, spaces.size)),
        ) { space ->
            SpaceRow(space = space) {
                webLayerModel.loadUrl(space.url)
            }
        }
    }
}