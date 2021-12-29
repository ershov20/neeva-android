package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.storage.Favicon
import com.neeva.app.ui.theme.NeevaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun SuggestionList(
    topSuggestion: NavSuggestion?,
    queryRowSuggestions: List<QueryRowSuggestion>,
    urlSuggestions: List<NavSuggestion>,
    faviconProvider: (Uri?) -> Flow<Favicon?>,
    onOpenUrl: (Uri) -> Unit,
    onEditUrl: (String) -> Unit
) {
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
                NavSuggestion(faviconProvider, onOpenUrl, it)
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
                    onLoadUrl = onOpenUrl,
                    onEditUrl = { onEditUrl(queryRowSuggestion.query) }
                )
            }

            items(
                urlSuggestions.filter { it.queryIndex == index },
                { "${it.url} ${it.queryIndex}" }
            ) {
                NavSuggestion(faviconProvider, onOpenUrl, it)
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
                NavSuggestion(faviconProvider, onOpenUrl, it)
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

@Preview(name = "1x font size", locale = "en")
@Preview(name = "2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "RTL, 1x font size", locale = "he")
@Preview(name = "RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SuggestionList_PreviewFullyLoaded() {
    NeevaTheme {
        SuggestionList(
            topSuggestion = NavSuggestion(
                url = Uri.parse("https://www.reddit.com"),
                label = stringResource(R.string.debug_long_string_primary),
                secondaryLabel = stringResource(R.string.debug_long_string_secondary)
            ),
            queryRowSuggestions = listOf(
                QueryRowSuggestion(
                    url = Uri.parse(""),
                    query = "Query 1",
                    drawableID = R.drawable.ic_baseline_search_24
                ),
                QueryRowSuggestion(
                    url = Uri.parse(""),
                    query = "Query 2",
                    drawableID = R.drawable.ic_baseline_search_24
                ),
                QueryRowSuggestion(
                    url = Uri.parse(""),
                    query = "Query 3",
                    drawableID = R.drawable.ic_baseline_search_24
                ),
            ),
            urlSuggestions = listOf(
                NavSuggestion(
                    url = Uri.parse("Suggestion 1"),
                    label = "Suggestion 1 for query 3",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    queryIndex = 2
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 2"),
                    label = "Suggestion 2 for query 3",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    queryIndex = 2
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 3"),
                    label = "Suggestion 1 for query 1",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    queryIndex = 0
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 4"),
                    label = "Unassociated suggestion #1",
                    secondaryLabel = "Suggestion 4"
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 5"),
                    label = "Unassociated suggestion #2",
                    secondaryLabel = "Suggestion 5"
                ),
            ),
            faviconProvider = { flowOf(null) },
            onOpenUrl = {}
        ) {}
    }
}