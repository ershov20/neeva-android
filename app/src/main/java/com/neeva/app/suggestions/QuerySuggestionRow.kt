// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun QuerySuggestionRow(
    suggestion: QueryRowSuggestion,
    onLoadUrl: (Uri) -> Unit,
    onEditUrl: (String) -> Unit,
    onLogSuggestionTap: ((SuggestionType) -> Unit)? = null
) {
    val onTapRow = {
        onLoadUrl(suggestion.url)
        if (onLogSuggestionTap != null) {
            onLogSuggestionTap(SuggestionType.QUERY_SUGGESTION)
        }
    }

    when {
        suggestion.annotationType == AnnotationType.Dictionary &&
            suggestion.dictionaryInfo?.word != null &&
            suggestion.dictionaryInfo.shortDefinition != null -> {
            DictionarySuggestionRow(
                onTapRow = onTapRow,
                word = suggestion.dictionaryInfo.word,
                shortDefinition = suggestion.dictionaryInfo.shortDefinition,
                phoneticSpelling = suggestion.dictionaryInfo.phoneticSpelling,
                lexicalCategory = suggestion.dictionaryInfo.lexicalCategory
            )
        }

        suggestion.annotationType == AnnotationType.Stock -> {
            suggestion.stockInfo?.let {
                StockSuggestionRow(
                    onTapRow = onTapRow,
                    companyName = it.companyName,
                    ticker = it.ticker,
                    currentPrice = it.currentPrice,
                    changeFromPreviousClose = it.changeFromPreviousClose,
                    percentChangeFromPreviousClose = it.percentChangeFromPreviousClose,
                    fetchedAtTime = it.fetchedAtTime
                )
            }
        }

        suggestion.annotationType == AnnotationType.Calculator -> {
            CalculatorSuggestionRow(
                suggestion = suggestion,
                onTapRow = onTapRow
            )
        }

        else -> {
            QueryNavSuggestionRow(
                query = suggestion.query,
                description = suggestion.description,
                imageURL = suggestion.imageURL,
                drawableID = suggestion.drawableID,
                drawableTint = MaterialTheme.colorScheme.onSurfaceVariant,
                onTapRow = onTapRow,
                onEditUrl = { onEditUrl(suggestion.query) }
            )
        }
    }
}
