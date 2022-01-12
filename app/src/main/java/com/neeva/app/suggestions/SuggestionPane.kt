package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.Site
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.ComposableSingletonEntryPoint
import com.neeva.app.zeroQuery.ZeroQuery
import dagger.hilt.EntryPoints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Composable
fun SuggestionPane(
    suggestionsModel: SuggestionsModel?,
    urlBarModel: URLBarModel,
    activeTabModel: ActiveTabModel
) {
    val entryPoint = EntryPoints.get(
        LocalContext.current.applicationContext,
        ComposableSingletonEntryPoint::class.java
    )
    val domainProvider = entryPoint.domainProvider()
    val historyManager = entryPoint.historyManager()

    val isUrlBarBlank by urlBarModel.userInputText.map { it.text.isBlank() }.collectAsState(true)
    val isLazyTab: Boolean by urlBarModel.isLazyTab.collectAsState()
    val domainSuggestions by historyManager.domainSuggestions.collectAsState()
    val siteSuggestions by historyManager.siteSuggestions.collectAsState()
    val currentURL: Uri by activeTabModel.urlFlow.collectAsState()

    val suggestionFlow = suggestionsModel?.suggestionFlow ?: MutableStateFlow(Suggestions())
    val suggestions by suggestionFlow.collectAsState()

    val topSuggestion = suggestions.autocompleteSuggestion
    val queryRowSuggestions = suggestions.queryRowSuggestions
    val queryNavSuggestions = suggestions.navSuggestions
    val historySuggestions = determineHistorySuggestions(
        domainSuggestions,
        siteSuggestions,
        domainProvider
    )

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
            faviconProvider = historyManager::getFaviconFlow,
            onOpenUrl = urlBarModel::loadUrl
        ) {
            updateUrlBarContents(urlBarModel, it)
        }
    } else {
        ZeroQuery(urlBarModel = urlBarModel) {
            if (!isLazyTab) {
                val favicon by historyManager.getFaviconFlow(currentURL).collectAsState(null)
                CurrentPageRow(favicon = favicon, url = currentURL) {
                    updateUrlBarContents(urlBarModel, currentURL.toString())
                }
            }
        }
    }
}

/** Create a combined list of places that the user has visited. */
internal fun determineHistorySuggestions(
    domainSuggestions: List<NavSuggestion>,
    historySuggestions: List<Site>,
    domainProvider: DomainProvider
): List<NavSuggestion> {
    // Prioritize the history suggestions first because they were directly visited by the user.
    val combinedSuggestions =
        historySuggestions.map { it.toNavSuggestion(domainProvider) } + domainSuggestions

    // Keep only the unique history items with unique URLs.
    return combinedSuggestions.distinctBy { it.url }
}

/** Updates what is being displayed in the URL bar to match the given string and focuses it for editing. */
private fun updateUrlBarContents(urlBarModel: URLBarModel, newContents: String) {
    urlBarModel.replaceLocationBarText(newContents)
}
