package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun QueryRowSuggestion(suggestion: QueryRowSuggestion, onLoadUrl: (Uri) -> Unit) {
    QueryRowSuggestion(
        query = suggestion.query,
        description = suggestion.description,
        imageURL = suggestion.imageURL,
        drawableID = suggestion.drawableID,
        row = true,
        onClick = { onLoadUrl(suggestion.url) }
    )
}

@Composable
fun QueryRowSuggestion(
    query: String,
    description: String? = null,
    imageURL: String? = null,
    drawableID: Int = R.drawable.ic_baseline_search_24,
    row: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(
                if (!row)
                    Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onClick() }
                        .border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp)
                else
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
            )
    ) {
        if (!imageURL.isNullOrEmpty()) {
            Image(
                painter = rememberImagePainter(
                    data = imageURL,
                    builder = {
                        crossfade(true)
                    }),
                contentDescription = "query image",
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            Image(
                imageVector = ImageVector.vectorResource(id = drawableID),
                contentDescription = "query image",
                modifier = Modifier
                    .then(
                        if (row) {
                            Modifier.padding(horizontal = 12.dp)
                        } else {
                            Modifier.padding(start = 8.dp)
                        }
                    )
                    .wrapContentHeight(Alignment.CenterVertically),
                colorFilter = ColorFilter.tint(Color.LightGray)
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = query,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
            )
            if (!description.isNullOrEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSecondary,
                    maxLines = 1,
                )
            }
        }
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
            row = true,
            onClick = {}
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
            row = true,
            onClick = {}
        )
    }
}

@Preview(name = "No description, not row, 1x font size")
@Preview(name = "No description, not row, 2x font size", fontScale = 2.0f)
@Composable
fun QuerySuggestion_PreviewNoImageUrlNoDescriptionNotRow() {
    NeevaTheme {
        QueryRowSuggestion(
            query = "search query",
            description = null,
            imageURL = null,
            drawableID = R.drawable.ic_baseline_search_24,
            row = false,
            onClick = {}
        )
    }
}