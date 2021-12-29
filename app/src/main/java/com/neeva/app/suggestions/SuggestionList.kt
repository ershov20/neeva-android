package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.history.DomainViewModel
import com.neeva.app.storage.Favicon
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.zeroQuery.ZeroQuery

@Composable
fun SuggestionList(
    suggestionsViewModel: SuggestionsViewModel,
    urlBarModel: URLBarModel,
    selectedTabModel: SelectedTabModel,
    domainViewModel: DomainViewModel,
    historyViewModel: HistoryViewModel
) {
    val topSuggestion by urlBarModel.autocompletedSuggestion.collectAsState()
    val queryRowSuggestions by suggestionsViewModel.queryRowSuggestions.collectAsState()
    val navSuggestions by suggestionsViewModel.navSuggestions.collectAsState()
    val domainSuggestions by domainViewModel.domainSuggestions.collectAsState()
    val showSuggestionList by suggestionsViewModel.shouldShowSuggestions.collectAsState()
    val currentURL: Uri by selectedTabModel.urlFlow.collectAsState()
    val isLazyTab: Boolean by urlBarModel.isLazyTab.collectAsState()

    if (showSuggestionList) {
        val urlSuggestions = navSuggestions + domainSuggestions

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

            topSuggestion?.let {
                item {
                    val favicon: Favicon? by domainViewModel.getFaviconFlow(it.url).collectAsState(null)
                    NavSuggestion(
                        faviconData = favicon?.toBitmap(),
                        onOpenUrl = urlBarModel::loadUrl,
                        navSuggestion = it
                    )
                }
            }

            item { SuggestionDivider() }

            // Display all queries with their associated navigations.
            item {
                SuggestionSectionHeader(stringRes = R.string.neeva_search)
            }

            queryRowSuggestions.forEachIndexed { index, queryRowSuggestion ->
                item {
                    QueryRowSuggestion(
                        suggestion = queryRowSuggestion,
                        onLoadUrl = urlBarModel::loadUrl,
                        onEditUrl = { updateUrlBarContents(urlBarModel, queryRowSuggestion.query) }
                    )
                }

                items(
                    urlSuggestions.filter { it.queryIndex == index },
                    { "${it.url} ${it.queryIndex}" }
                ) {
                    val favicon: Favicon? by domainViewModel.getFaviconFlow(it.url).collectAsState(null)
                    NavSuggestion(
                        faviconData = favicon?.toBitmap(),
                        onOpenUrl = urlBarModel::loadUrl,
                        navSuggestion = it
                    )
                }

                if (index != queryRowSuggestions.size - 1) {
                    item { SuggestionDivider() }
                }
            }

            // Display all the suggestions that are unassociated with a query that was displayed.
            // This might happen because we show only the top 3 possible queries.
            val unassociatedSuggestions = urlSuggestions.filter {
                it.queryIndex == null || it.queryIndex >= queryRowSuggestions.size
            }
            if (unassociatedSuggestions.isNotEmpty()) {
                item { SuggestionDivider() }

                items(
                    unassociatedSuggestions,
                    { "${it.url} ${it.queryIndex}" }
                ) {
                    val favicon: Favicon? by domainViewModel.getFaviconFlow(it.url).collectAsState(null)
                    NavSuggestion(
                        faviconData = favicon?.toBitmap(),
                        onOpenUrl = urlBarModel::loadUrl,
                        navSuggestion = it
                    )
                }
            }
        }
    } else {
        ZeroQuery(
            urlBarModel = urlBarModel,
            historyViewModel = historyViewModel
        ) {
            if (!isLazyTab) {
                CurrentPageRow(
                    domainViewModel = domainViewModel,
                    url = currentURL
                ) {
                    updateUrlBarContents(urlBarModel, currentURL.toString())
                }
            }
        }
    }
}

private fun updateUrlBarContents(urlBarModel: URLBarModel, newContents: String) {
    urlBarModel.onRequestFocus()
    urlBarModel.onLocationBarTextChanged(
        urlBarModel.text.value.copy(
            newContents,
            TextRange(newContents.length, newContents.length)
        )
    )
}

@Composable
fun SuggestionDivider() {
    Box(
        Modifier
            .height(8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    )
}