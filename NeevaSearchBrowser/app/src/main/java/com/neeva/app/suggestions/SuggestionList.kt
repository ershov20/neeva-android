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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.map
import com.neeva.app.R
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.zeroQuery.ZeroQuery
import com.neeva.app.zeroQuery.ZeroQueryViewModel

@Composable
fun SuggestionList(
    suggestionsViewModel: SuggestionsViewModel,
    urlBarModel: URLBarModel,
    selectedTabModel: SelectedTabModel,
    domainViewModel: DomainViewModel,
    historyViewModel: HistoryViewModel,
    zeroQueryViewModel: ZeroQueryViewModel
) {
    val topSuggestion by suggestionsViewModel.topSuggestion.observeAsState()
    val queryRowSuggestions by suggestionsViewModel.queryRowSuggestions.observeAsState(emptyList())
    val navSuggestions by suggestionsViewModel.navSuggestions.observeAsState(emptyList())
    val domainSuggestions by domainViewModel.domainsSuggestions.observeAsState(emptyList())
    val showSuggestionList by suggestionsViewModel.shouldShowSuggestions.observeAsState(false)
    val currentURL: Uri? by selectedTabModel.currentUrl.observeAsState()
    val loadUrl: (Uri) -> Unit by zeroQueryViewModel.isLazyTab
        .map { isLazyTab ->
            { uri: Uri -> selectedTabModel.loadUrl(uri, isLazyTab) }
        }
        .observeAsState { uri:Uri -> selectedTabModel.loadUrl(uri) }

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

            item { SuggestionDivider() }

            // Display all queries with their associated navigations.
            item {
                SuggestionSectionHeader(stringRes = R.string.neeva_search)
            }

            queryRowSuggestions.forEachIndexed { index, queryRowSuggestion ->
                item {
                    QueryRowSuggestion(suggestion = queryRowSuggestion, onLoadUrl = loadUrl)
                }

                items(
                    urlSuggestions.filter { it.queryIndex == index },
                    { "${it.url} ${it.queryIndex}" }
                ) {
                    val bitmap: Bitmap? by domainViewModel.getFaviconFor(it.url).observeAsState()
                    NavSuggestion(
                        faviconData = bitmap,
                        onOpenUrl = loadUrl,
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
                    val bitmap: Bitmap? by domainViewModel.getFaviconFor(it.url).observeAsState()
                    NavSuggestion(
                        faviconData = bitmap,
                        onOpenUrl = loadUrl,
                        navSuggestion = it
                    )
                }
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
                url = currentURL!!
            ) {
                urlBarModel.onRequestFocus()
                val currentURLText = currentURL?.toString() ?: return@CurrentPageRow
                urlBarModel.onLocationBarTextChanged(
                    urlBarModel.text.value!!.copy(
                        currentURLText,
                        TextRange(currentURLText.length, currentURLText.length)
                    )
                )
            }
        }
    }
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