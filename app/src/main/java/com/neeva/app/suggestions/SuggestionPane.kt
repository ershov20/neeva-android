package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.storage.Favicon
import com.neeva.app.storage.Site
import com.neeva.app.storage.SpaceStore
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.zeroQuery.ZeroQuery
import kotlinx.coroutines.flow.map

@Composable
fun SuggestionPane(
    suggestionsModel: SuggestionsModel,
    urlBarModel: URLBarModel,
    activeTabModel: ActiveTabModel,
    historyManager: HistoryManager,
    spaceStore: SpaceStore
) {
    val isUrlBarBlank: Boolean by urlBarModel.userInputText.map { it.text.isBlank() }.collectAsState(true)
    val isLazyTab: Boolean by urlBarModel.isLazyTab.collectAsState()
    val domainSuggestions by historyManager.domainSuggestions.collectAsState()
    val siteSuggestions by historyManager.siteSuggestions.collectAsState()
    val currentURL: Uri by activeTabModel.urlFlow.collectAsState()
    val suggestions by suggestionsModel.suggestionFlow.collectAsState()

    val topSuggestion = suggestions.autocompleteSuggestion
    val queryRowSuggestions = suggestions.queryRowSuggestions
    val queryNavSuggestions = suggestions.navSuggestions
    val historySuggestions = determineHistorySuggestions(domainSuggestions, siteSuggestions)

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
        ZeroQuery(
            urlBarModel = urlBarModel,
            historyManager = historyManager,
            spaceStore = spaceStore
        ) {
            if (!isLazyTab) {
                val favicon: Favicon? by historyManager.getFaviconFlow(currentURL).collectAsState(null)
                CurrentPageRow(favicon = favicon?.toBitmap(), url = currentURL) {
                    updateUrlBarContents(urlBarModel, currentURL.toString())
                }
            }
        }
    }
}

/** Create a combined list of places that the user has visited. */
internal fun determineHistorySuggestions(
    domainSuggestions: List<NavSuggestion>,
    historySuggestions: List<Site>
) : List<NavSuggestion> {
    // Prioritize the history suggestions first because they were directly visited by the user.
    val combinedSuggestions = historySuggestions.map { it.toNavSuggestion() } + domainSuggestions

    // Keep only the unique history items with unique URLs.
    return combinedSuggestions.distinctBy { it.url }
}

/** Updates what is being displayed in the URL bar to match the given string and focuses it for editing. */
private fun updateUrlBarContents(urlBarModel: URLBarModel, newContents: String) {
    urlBarModel.replaceLocationBarText(newContents)
}
