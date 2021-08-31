package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.neeva.app.R
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.storage.Space
import com.neeva.app.storage.SpaceStore
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.web.SelectedTabModel
import com.neeva.app.widgets.FaviconView
import com.neeva.app.zeroQuery.ZeroQuery
import com.neeva.app.zeroQuery.ZeroQueryViewModel

@Composable
fun SuggestionList(suggestionsViewModel: SuggestionsViewModel,
                   urlBarModel: URLBarModel,
                   selectedTabModel: SelectedTabModel,
                   domainViewModel: DomainViewModel,
                   historyViewModel: HistoryViewModel,
                   zeroQueryViewModel: ZeroQueryViewModel
) {
    val topSuggestions by suggestionsViewModel.topSuggestions.observeAsState(emptyList())
    val queryRowSuggestions by suggestionsViewModel.queryRowSuggestions.observeAsState(emptyList())
    val navSuggestions by suggestionsViewModel.navSuggestions.observeAsState(emptyList())
    val domainSuggestions by domainViewModel.domainsSuggestions.observeAsState(emptyList())
    val showSuggestionList by suggestionsViewModel.shouldShowSuggestions.observeAsState(false)
    val currentURL: Uri? by selectedTabModel.currentUrl.observeAsState()
    val currentTitle: String by selectedTabModel.currentTitle.observeAsState("")
    val loadUrl: (Uri) -> Unit by zeroQueryViewModel.isLazyTab.map { isLazyTab ->
            { uri:Uri -> selectedTabModel.loadUrl(uri, isLazyTab)}
        }.observeAsState { uri:Uri -> selectedTabModel.loadUrl(uri)}

    if  (showSuggestionList) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary)
        ) {
            item {
                Box(
                    Modifier
                        .height(2.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background))
            }
            items(topSuggestions,
                key = { suggestion -> suggestion.url}) {
                NavSuggestView(
                    navSuggestion = it,
                    onOpenUrl = loadUrl,
                    faviconData = domainViewModel.getFaviconFor(it.url)
                )
            }
            item {
                Box(
                    Modifier
                        .height(8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background))
            }
            item {
                QueryChipSuggestions(
                    suggestionsViewModel = suggestionsViewModel,
                    onLoadUrl = loadUrl)
            }
            items(queryRowSuggestions,
            key = { suggestion -> suggestion.url}) {
                QueryRowSuggestion(suggestion = it, onLoadUrl = loadUrl)
            }
            item {
                Box(
                    Modifier
                        .height(8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background))
            }
            items(navSuggestions + domainSuggestions,
                key = { suggestion -> suggestion.url }) {
                NavSuggestView(
                    navSuggestion = it,
                    onOpenUrl = loadUrl,
                    faviconData = domainViewModel.getFaviconFor(it.url),
                )
            }
        }
    } else {
        ZeroQuery(
            selectedTabModel = selectedTabModel,
            historyViewModel = historyViewModel,
            zeroQueryViewModel = zeroQueryViewModel
        ) {
            CurrentPageRow(
                domainViewModel = domainViewModel,
                url = currentURL!!, title = currentTitle,
                onEditPressed = {
                    urlBarModel.onRequestFocus()
                    val currentURLText = currentURL?.toString() ?: return@CurrentPageRow
                    urlBarModel.onLocationBarTextChanged(urlBarModel.text.value!!.copy(currentURLText,
                        TextRange(currentURLText.length, currentURLText.length)))
                }
            )
        }
    }
}

@Composable
fun QueryChipSuggestions(suggestionsViewModel: SuggestionsViewModel, onLoadUrl: (Uri) -> Unit) {
    val queryChipSuggestions by suggestionsViewModel.queryChipSuggestions.observeAsState(emptyList())
    val firstRow = queryChipSuggestions.slice(queryChipSuggestions.indices step 2)
    val secondRow = queryChipSuggestions.slice(1 until queryChipSuggestions.size step 2)

    Column {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary)
        ) {
            items(firstRow) {
                QuerySuggestion(
                    query = it.query) {
                    onLoadUrl(it.url)
                }
            }
        }
        if (secondRow.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary)
            ) {
                items(secondRow) {
                    QuerySuggestion(query = it.query) {
                        onLoadUrl(it.url)
                    }
                }
            }
        }
    }
}


@Composable
fun CurrentPageRow(domainViewModel: DomainViewModel, url: Uri, title: String, onEditPressed: () -> Unit) {
    val spaces: List<Space> by SpaceStore.shared.allSpacesFlow.asLiveData().observeAsState(emptyList())

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(start = 12.dp)
    ) {
        FaviconView(domainViewModel.getFaviconFor(url))
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1.0f)
        ) {
            Text(
                text = url.toString(),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
            )
            Text(
                text = "Edit current url",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSecondary,
                maxLines = 1,
            )
        }
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_edit_24),
            contentDescription = "query icon",
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .requiredSize(48.dp, 48.dp)
                .clickable { onEditPressed() },
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
        )
    }
}