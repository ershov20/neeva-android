package com.neeva.app.suggestions

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.zeroquery.IncognitoZeroQuery
import com.neeva.app.zeroquery.RegularProfileZeroQuery
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SuggestionPane(modifier: Modifier = Modifier, isFirstRun: Boolean) {
    val historyManager = LocalEnvironment.current.historyManager
    val neevaConstants = LocalEnvironment.current.neevaConstants

    val browserWrapper = LocalBrowserWrapper.current
    val urlBarModel = browserWrapper.urlBarModel
    val faviconCache = browserWrapper.faviconCache
    val suggestionsModel = browserWrapper.suggestionsModel

    val isUrlBarBlank by urlBarModel.isUserQueryBlank.collectAsState(true)
    val historySuggestions by historyManager.historySuggestions.collectAsState()

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
                    onOpenUrl = browserWrapper::loadUrl,
                    onEditUrl = {
                        urlBarModel.replaceLocationBarText(it)
                    },
                    onLogSuggestionTap = suggestionsModel?.let { it::logSuggestionTap }
                )
            }

            browserWrapper.isIncognito -> {
                IncognitoZeroQuery {
                    CurrentPageRow(browserWrapper)
                }
            }

            else -> {
                RegularProfileZeroQuery(
                    urlBarModel = urlBarModel,
                    isFirstRun = isFirstRun,
                    neevaConstants = neevaConstants
                ) {
                    CurrentPageRow(browserWrapper)
                }
            }
        }
    }
}
