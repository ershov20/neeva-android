// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

@Composable
fun FirstRunZeroQuery(
    topContent: @Composable () -> Unit
) {
    val suggestionsUI = searchSuggestions()

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .semantics { testTag = "RegularProfileZeroQuery" }
    ) {
        item {
            topContent()
        }

        if (suggestionsUI != null) this.suggestionsUI()

        item {
            AdBlockerPromo()
        }

        item {
            NeevaPromo()
        }
    }
}
