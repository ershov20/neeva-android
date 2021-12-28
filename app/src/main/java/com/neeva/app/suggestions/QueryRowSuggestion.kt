package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun QueryRowSuggestion(
    suggestion: QueryRowSuggestion,
    onLoadUrl: (Uri) -> Unit,
    onEditUrl: (() -> Unit)? = null
) {
    QueryRowSuggestion(
        query = suggestion.query,
        description = suggestion.description,
        imageURL = suggestion.imageURL,
        drawableID = suggestion.drawableID,
        onTapRow = { onLoadUrl(suggestion.url) },
        onEditUrl = onEditUrl
    )
}

@Composable
fun QueryRowSuggestion(
    query: String,
    description: String? = null,
    imageURL: String? = null,
    drawableID: Int = R.drawable.ic_baseline_search_24,
    onTapRow: () -> Unit,
    onEditUrl: (() -> Unit)? = null
) {
    SuggestionRow(
        primaryLabel = query,
        onTapRow = { onTapRow.invoke() },
        secondaryLabel = description,
        onTapEdit = onEditUrl,
        faviconData = null,
        imageURL = imageURL,
        drawableID = drawableID
    )
}

@Preview(name = "Image URL non-null, 1x font size")
@Preview(name = "Image URL non-null, 2x font size", fontScale = 2.0f)
@Composable
fun QuerySuggestion_PreviewWithImageUrl() {
    NeevaTheme {
        QueryRowSuggestion(
            query = "search query",
            description = "Suggestion description",
            imageURL = "https://www.neeva.com/favicon.png",
            drawableID = R.drawable.ic_baseline_search_24,
            onTapRow = {},
            onEditUrl = {}
        )
    }
}

@Preview(name = "1x font size")
@Preview(name = "2x font size", fontScale = 2.0f)
@Composable
fun QuerySuggestion_PreviewNoImageUrl() {
    NeevaTheme {
        QueryRowSuggestion(
            query = "search query",
            description = "Suggestion description",
            imageURL = null,
            drawableID = R.drawable.ic_baseline_search_24,
            onTapRow = {},
            onEditUrl = {}
        )
    }
}

@Preview(name = "No description, 1x font size")
@Preview(name = "No description, 2x font size", fontScale = 2.0f)
@Composable
fun QuerySuggestion_PreviewNoImageUrlNoDescription() {
    NeevaTheme {
        QueryRowSuggestion(
            query = "search query",
            description = null,
            imageURL = null,
            drawableID = R.drawable.ic_baseline_search_24,
            onTapRow = {},
            onEditUrl = {}
        )
    }
}

@Preview(name = "No image URL, 1x font size")
@Preview(name = "No image URL, 2x font size", fontScale = 2.0f)
@Composable
fun QuerySuggestion_PreviewNoImageUrlNoEdit() {
    NeevaTheme {
        QueryRowSuggestion(
            query = "search query",
            description = "Suggestion description",
            imageURL = null,
            drawableID = R.drawable.ic_baseline_search_24,
            onTapRow = {},
            onEditUrl = null
        )
    }
}

@Preview(name = "Uneditable, no description, 1x font size")
@Preview(name = "Uneditable, no description, 2x font size", fontScale = 2.0f)
@Composable
fun QuerySuggestion_PreviewNoImageUrlNoDescriptionNoEdit() {
    NeevaTheme {
        QueryRowSuggestion(
            query = "search query",
            description = null,
            imageURL = null,
            drawableID = R.drawable.ic_baseline_search_24,
            onTapRow = {},
            onEditUrl = null
        )
    }
}