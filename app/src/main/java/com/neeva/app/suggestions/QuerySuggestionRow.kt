package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import com.neeva.app.ui.theme.SuggestionDrawableTint

@Composable
fun QuerySuggestionRow(
    suggestion: QueryRowSuggestion,
    onLoadUrl: (Uri) -> Unit,
    onEditUrl: (() -> Unit)? = null
) {
    val onTapRow = { onLoadUrl(suggestion.url) }

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
                drawableTint = SuggestionDrawableTint,
                onTapRow = onTapRow,
                onEditUrl = onEditUrl
            )
        }
    }
}
