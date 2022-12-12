// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalRegularProfileZeroQueryViewModel
import com.neeva.app.R
import com.neeva.app.suggestions.QuerySuggestionRow
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.collapsingsection.collapsingSection

// This has a somewhat awkward structure because we need this to be callable from a @Composable
// function, but Compose's implementation of LazyColumn does not make the content lambda composable.
// The workaround is that this function is called outside of the LazyColumn, and it returns a lambda
// that can then be invoked inside the content lambda.
@Composable
fun searchSuggestions(): (LazyListScope.() -> Unit)? {
    val browserWrapper = LocalBrowserWrapper.current
    val zeroQueryModel = LocalRegularProfileZeroQueryViewModel.current
    val suggestions by zeroQueryModel.suggestedSearches.collectAsState()
    val expanded by zeroQueryModel.isSuggestedQueriesExpanded.collectAsState()

    if (suggestions.isEmpty()) return null

    return {
        item {
            Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
        }

        collapsingSection(
            label = R.string.searches,
            collapsingSectionState = expanded,
            onUpdateCollapsingSectionState = {
                zeroQueryModel.advanceState(ZeroQueryPrefs.SuggestedQueriesState)
            }
        ) {
            items(suggestions) { search ->
                QuerySuggestionRow(
                    suggestion = search,
                    onLoadUrl = browserWrapper::loadUrl,
                    onEditUrl = browserWrapper.urlBarModel::replaceLocationBarText
                )
            }
        }
    }
}
