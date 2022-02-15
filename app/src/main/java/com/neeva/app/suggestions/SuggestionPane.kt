package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.zeroQuery.IncognitoZeroQuery
import com.neeva.app.zeroQuery.ZeroQuery
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SuggestionPane(modifier: Modifier = Modifier) {
    val historyManager = LocalEnvironment.current.historyManager

    val browserWrapper = LocalBrowserWrapper.current
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

    Surface(modifier = modifier) {
        when {
            showSuggestionList -> {
                SuggestionList(
                    topSuggestion = topSuggestion,
                    queryRowSuggestions = queryRowSuggestions,
                    queryNavSuggestions = queryNavSuggestions,
                    historySuggestions = historySuggestions,
                    faviconCache = faviconCache,
                    onOpenUrl = urlBarModel::loadUrl
                ) {
                    urlBarModel.replaceLocationBarText(it)
                }
            }

            browserWrapper.isIncognito -> {
                IncognitoZeroQuery()
            }

            else -> {
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
                            urlBarModel.replaceLocationBarText(
                                if (isShowingQuery) displayedText else currentURL.toString()
                            )
                        }
                    }
                }
            }
        }
    }
}
