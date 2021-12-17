package com.neeva.app.suggestions

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SuggestionRow(
    primaryLabel: String,
    onTapRow: () -> Unit,
    secondaryLabel: String? = null,
    onTapEdit: (() -> Unit)? = null,
    faviconData: Bitmap? = null,
    imageURL: String? = null,
    drawableID: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTapRow.invoke() }
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier.padding(start = 8.dp)
        when {
            !imageURL.isNullOrBlank() -> {
                Image(
                    painter = rememberImagePainter(
                        data = imageURL,
                        builder = { crossfade(true) }
                    ),
                    contentDescription = null,
                    modifier = iconModifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                )
            }

            drawableID != null -> {
                Image(
                    imageVector = ImageVector.vectorResource(id = drawableID),
                    contentDescription = null,
                    modifier = iconModifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.LightGray)
                )
            }

            else -> {
                FaviconView(
                    faviconData,
                    modifier = iconModifier
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            secondaryLabel?.let {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSecondary,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (onTapEdit != null) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_north_west_24),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .requiredSize(48.dp, 48.dp)
                    .clickable { onTapEdit.invoke() },
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
        }
    }
}

@Preview("No edit, 1x")
@Preview("No edit, 2x", fontScale = 2.0f)
@Composable
fun SuggestionRow_Preview() {
    NeevaTheme {
        SuggestionRow(
            primaryLabel = "Primary label",
            onTapRow = {},
            secondaryLabel = "Secondary label",
            onTapEdit = null,
            faviconData = null
        )
    }
}

@Preview("No edit, 1x")
@Preview("No edit, 2x", fontScale = 2.0f)
@Preview
@Composable
fun SuggestionRow_PreviewEditable() {
    NeevaTheme {
        SuggestionRow(
            primaryLabel = "Primary label",
            onTapRow = {},
            secondaryLabel = "Secondary label",
            onTapEdit = { },
            faviconData = null
        )
    }
}
