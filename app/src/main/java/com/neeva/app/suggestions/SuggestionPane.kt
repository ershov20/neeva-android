package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.neeva.app.LocalEnvironment
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.ComposableSingletonEntryPoint
import com.neeva.app.zeroQuery.IncognitoZeroQuery
import com.neeva.app.zeroQuery.ZeroQuery
import dagger.hilt.EntryPoints
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SuggestionPane() {
    val historyManager = EntryPoints
        .get(LocalContext.current.applicationContext, ComposableSingletonEntryPoint::class.java)
        .historyManager()

    val browserWrapper = LocalEnvironment.current.browserWrapper
    val urlBarModel = browserWrapper.urlBarModel
    val activeTabModel = browserWrapper.activeTabModel
    val faviconCache = browserWrapper.faviconCache
    val suggestionsModel = browserWrapper.suggestionsModel

    val isUrlBarBlank by urlBarModel.isUserQueryBlank.collectAsState(true)
    val isLazyTab: Boolean by urlBarModel.isLazyTab.collectAsState()
    val historySuggestions by historyManager.historySuggestions.collectAsState()
    val currentURL: Uri by activeTabModel.urlFlow.collectAsState()
    val displayedText: String by activeTabModel.displayedText.collectAsState()
    val isShowingQuery: Boolean by activeTabModel.isShowingQuery.collectAsState()

    val faviconBitmap by faviconCache.getFaviconAsync(currentURL)

    val suggestionFlow = suggestionsModel?.suggestionFlow ?: MutableStateFlow(Suggestions())
    val suggestions by suggestionFlow.collectAsState()

    val topSuggestion = suggestions.autocompleteSuggestion
    val queryRowSuggestions = suggestions.queryRowSuggestions
    val queryNavSuggestions = suggestions.navSuggestions

    val showSuggestionList = when {
        // Don't show suggestions if the user hasn't typed anything.
        isUrlBarBlank -> false

        // If anything can be displayed to the user, show it.
        topSuggestion != null -> true
        queryRowSuggestions.isNotEmpty() -> true
        queryNavSuggestions.isNotEmpty() -> true
        historySuggestions.isNotEmpty() -> true

        // Show the Zero Query UI instead.
        else -> false
    }

    if (showSuggestionList) {
        SuggestionList(
            topSuggestion = topSuggestion,
            queryRowSuggestions = queryRowSuggestions,
            queryNavSuggestions = queryNavSuggestions,
            historySuggestions = historySuggestions,
            faviconCache = faviconCache,
            onOpenUrl = urlBarModel::loadUrl
        ) {
            updateUrlBarContents(urlBarModel, it)
        }
    } else if (browserWrapper.isIncognito) {
        IncognitoZeroQuery()
    } else {
        ZeroQuery(urlBarModel = urlBarModel, faviconCache = faviconCache) {
            if (!isLazyTab && currentURL.toString().isNotBlank()) {
                CurrentPageRow(
                    faviconBitmap = faviconBitmap,
                    label = if (isShowingQuery) {
                        displayedText
                    } else {
                        currentURL.toString()
                    },
                    isShowingQuery = isShowingQuery
                ) {
                    updateUrlBarContents(
                        urlBarModel,
                        if (isShowingQuery) displayedText else currentURL.toString()
                    )
                }
            }
        }
    }
}

/** Updates what is being displayed in the URL bar to match the given string and focuses it for editing. */
private fun updateUrlBarContents(urlBarModel: URLBarModel, newContents: String) {
    urlBarModel.replaceLocationBarText(newContents)
}
