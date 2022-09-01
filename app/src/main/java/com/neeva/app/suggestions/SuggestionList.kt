// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.R
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.previewFaviconCache
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.DefaultDivider

@Composable
fun SuggestionList(
    topSuggestion: NavSuggestion?,
    queryRowSuggestions: List<QueryRowSuggestion>,
    queryNavSuggestions: List<NavSuggestion>,
    historySuggestions: List<NavSuggestion>,
    faviconCache: FaviconCache,
    onOpenUrl: (Uri) -> Unit,
    onEditUrl: (String) -> Unit,
    onLogSuggestionTap: ((SuggestionType) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .semantics { testTag = "SuggestionList" }
    ) {
        topSuggestion?.let {
            item {
                NavSuggestion(faviconCache, onOpenUrl, onLogSuggestionTap, it)
            }

            item {
                DefaultDivider()
            }
        }

        // Display search results.
        if (queryRowSuggestions.isNotEmpty() || queryNavSuggestions.isNotEmpty()) {
            // Display all queries with their associated navigations.
            item {
                SuggestionHeader(R.string.neeva_search)
            }

            queryRowSuggestions.forEachIndexed { index, queryRowSuggestion ->
                item {
                    QuerySuggestionRow(
                        suggestion = queryRowSuggestion,
                        onLoadUrl = onOpenUrl,
                        onEditUrl = { onEditUrl(queryRowSuggestion.query) },
                        onLogSuggestionTap = onLogSuggestionTap
                    )
                }

                items(
                    queryNavSuggestions.filter { it.queryIndex == index },
                    { "${it.url} ${it.queryIndex}" }
                ) {
                    NavSuggestion(faviconCache, onOpenUrl, onLogSuggestionTap, it)
                }

                if (index != queryRowSuggestions.size - 1) {
                    item { DefaultDivider() }
                }
            }

            // Display all the suggestions that are unassociated with a query that was displayed.
            // This might happen because we show only the top 3 possible queries.
            val unassociatedSuggestions = queryNavSuggestions.filter {
                it.queryIndex == null || it.queryIndex >= queryRowSuggestions.size
            }
            if (unassociatedSuggestions.isNotEmpty()) {
                item { DefaultDivider() }

                items(
                    unassociatedSuggestions,
                    { "${it.url} ${it.queryIndex}" }
                ) {
                    NavSuggestion(faviconCache, onOpenUrl, onLogSuggestionTap, it)
                }
            }

            item {
                DefaultDivider()
            }
        }

        // Display results from the user's navigation history.
        if (historySuggestions.isNotEmpty()) {
            // Display all queries with their associated navigations.
            item {
                SuggestionHeader(R.string.history)
            }

            items(
                historySuggestions,
                { "${it.url} ${it.queryIndex}" }
            ) {
                NavSuggestion(faviconCache, onOpenUrl, onLogSuggestionTap, it)
            }
        }
    }
}

@Composable
fun SuggestionHeader(stringId: Int) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(stringId),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = Dimensions.PADDING_LARGE,
                vertical = Dimensions.PADDING_SMALL
            ),
            overflow = TextOverflow.Ellipsis
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun SuggestionList_PreviewFullyLoaded_ShowSearchSuggestions() {
    NeevaThemePreviewContainer(
        useDarkTheme = false,
        addBorder = false
    ) {
        SuggestionList(
            topSuggestion = NavSuggestion(
                url = Uri.parse("https://www.reddit.com"),
                label = stringResource(R.string.debug_long_string_primary),
                secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                type = SuggestionType.AUTOCOMPLETE_SUGGESTION
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
            queryNavSuggestions = listOf(
                NavSuggestion(
                    url = Uri.parse("Suggestion 1"),
                    label = "Suggestion 1 for query 3",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    queryIndex = 2,
                    type = SuggestionType.MEMORIZED_SUGGESTION
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 2"),
                    label = "Suggestion 2 for query 3",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    queryIndex = 2,
                    type = SuggestionType.MEMORIZED_SUGGESTION
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 3"),
                    label = "Suggestion 1 for query 1",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    queryIndex = 0,
                    type = SuggestionType.MEMORIZED_SUGGESTION
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 4"),
                    label = "Unassociated suggestion #1",
                    secondaryLabel = "Suggestion 4",
                    type = SuggestionType.MEMORIZED_SUGGESTION
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 5"),
                    label = "Unassociated suggestion #2",
                    secondaryLabel = "Suggestion 5",
                    type = SuggestionType.MEMORIZED_SUGGESTION
                ),
            ),
            historySuggestions = listOf(
                NavSuggestion(
                    url = Uri.parse("Suggestion 1"),
                    label = "History suggestion 1",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    type = SuggestionType.HISTORY_SUGGESTION
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 2"),
                    label = "History suggestion 2",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    type = SuggestionType.HISTORY_SUGGESTION
                ),
                NavSuggestion(
                    url = Uri.parse("Suggestion 3"),
                    label = "History suggestion 3",
                    secondaryLabel = stringResource(R.string.debug_long_string_secondary),
                    type = SuggestionType.HISTORY_SUGGESTION
                ),
            ),
            faviconCache = previewFaviconCache,
            onOpenUrl = {},
            onEditUrl = {}
        )
    }
}
