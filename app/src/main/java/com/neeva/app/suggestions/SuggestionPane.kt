package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextRange
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.history.DomainViewModel
import com.neeva.app.history.HistoryViewModel
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
    val isLazyTab: Boolean by urlBarModel.isLazyTab.collectAsState()
    val domainSuggestions by domainViewModel.domainSuggestions.collectAsState()
    val currentURL: Uri by selectedTabModel.urlFlow.collectAsState()
    val suggestions by suggestionsViewModel.suggestionFlow.collectAsState()

    val topSuggestion = suggestions.autocompleteSuggestion
    val queryRowSuggestions = suggestions.queryRowSuggestions
    val navSuggestions = suggestions.navSuggestions
    val showSuggestionList = suggestions.shouldShowSuggestions

    if (showSuggestionList) {
        SuggestionList(
            topSuggestion = topSuggestion,
            queryRowSuggestions = queryRowSuggestions,
            urlSuggestions = navSuggestions + domainSuggestions,
            faviconProvider = domainViewModel::getFaviconFlow,
            onOpenUrl = urlBarModel::loadUrl
        ) {
            updateUrlBarContents(urlBarModel, it)
        }
    } else {
        ZeroQuery(
            urlBarModel = urlBarModel,
            historyViewModel = historyViewModel
        ) {
            if (!isLazyTab) {
                val favicon: Favicon? by domainViewModel.getFaviconFlow(currentURL).collectAsState(null)
                CurrentPageRow(favicon = favicon?.toBitmap(), url = currentURL) {
                    updateUrlBarContents(urlBarModel, currentURL.toString())
                }
            }
        }
    }
}

/** Updates what is being displayed in the URL bar to match the given string and focuses it for editing. */
private fun updateUrlBarContents(urlBarModel: URLBarModel, newContents: String) {
    urlBarModel.onRequestFocus()
    urlBarModel.onLocationBarTextChanged(
        urlBarModel.text.value.copy(
            newContents,
            TextRange(newContents.length, newContents.length)
        )
    )
}
