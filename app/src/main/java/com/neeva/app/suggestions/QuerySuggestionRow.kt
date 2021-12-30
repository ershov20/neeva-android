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
        suggestion.annotationType == AnnotationType.Dictionary
                && suggestion.dictionaryInfo?.word != null
                && suggestion.dictionaryInfo.shortDefinition != null -> {
            DictionarySuggestionRow(
                onTapRow = onTapRow,
                word = suggestion.dictionaryInfo.word,
                shortDefinition = suggestion.dictionaryInfo.shortDefinition,
                phoneticSpelling = suggestion.dictionaryInfo.phoneticSpelling,
                lexicalCategory = suggestion.dictionaryInfo.lexicalCategory
            )
        }

        suggestion.annotationType == AnnotationType.Stock -> {
            StockSuggestionRow(
                onTapRow = onTapRow,
                companyName = suggestion.stockInfo?.companyName,
                ticker = suggestion.stockInfo?.ticker,
                currentPrice = suggestion.stockInfo?.currentPrice,
                changeFromPreviousClose = suggestion.stockInfo?.changeFromPreviousClose,
                percentChangeFromPreviousClose = suggestion.stockInfo?.percentChangeFromPreviousClose,
                fetchedAtTime = suggestion.stockInfo?.fetchedAtTime
            )
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