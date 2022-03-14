package com.neeva.app.suggestions

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

/**
 * Base skeleton for everything that can be displayed as a suggestion in UI.  Callers must provide
 * a Composable [mainContent] that applies the provided modifier in order for it to properly take
 * the fully available width in the row.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun BaseSuggestionRow(
    onTapRow: () -> Unit,
    onTapRowContentDescription: String? = null,
    onTapEdit: (() -> Unit)? = null,
    faviconBitmap: Bitmap? = null,
    imageURL: String? = null,
    drawableID: Int? = null,
    drawableTint: Color? = null,
    mainContent: @Composable (modifier: Modifier) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = onTapRowContentDescription) {
                onTapRow.invoke()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .padding(start = Dimensions.PADDING_MEDIUM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier
            when {
                !imageURL.isNullOrBlank() -> {
                    Image(
                        painter = rememberImagePainter(
                            data = imageURL,
                            builder = { crossfade(true) }
                        ),
                        contentDescription = null,
                        modifier = iconModifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                drawableID != null -> {
                    Image(
                        painter = painterResource(drawableID),
                        contentDescription = null,
                        modifier = iconModifier.size(20.dp),
                        colorFilter = drawableTint?.let { ColorFilter.tint(it) }
                    )
                }

                else -> {
                    FaviconView(
                        bitmap = faviconBitmap,
                        modifier = iconModifier
                    )
                }
            }

            // Keep the icon away from the content.
            Spacer(modifier = Modifier.width(Dimensions.PADDING_MEDIUM))

            mainContent(Modifier.weight(1.0f))

            if (onTapEdit != null) {
                IconButton(onClick = onTapEdit) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_north_west_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // If there's no edit icon, add an extra spacer to prevent the content from hitting
                // the edge.
                Spacer(modifier = Modifier.width(Dimensions.PADDING_MEDIUM))
            }
        }
    }
}

@Preview("No edit, 1x", locale = "en")
@Preview("No edit, 2x", locale = "en", fontScale = 2.0f)
@Preview("No edit, RTL, 1x", locale = "he")
@Preview("No edit, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun BaseSuggestionRow_Preview() {
    NeevaTheme {
        BaseSuggestionRow(
            onTapRow = {},
            onTapEdit = null,
            faviconBitmap = null
        ) {
            Box(
                modifier = it
                    .background(Color.Magenta)
                    .height(56.dp)
            )
        }
    }
}

@Preview("With edit, 1x")
@Preview("With edit, 2x", fontScale = 2.0f)
@Preview("With edit, RTL, 1x", locale = "he")
@Preview("With edit, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun BaseSuggestionRow_PreviewEditable() {
    NeevaTheme {
        BaseSuggestionRow(
            onTapRow = {},
            onTapEdit = {},
            faviconBitmap = null
        ) {
            Box(
                modifier = it
                    .background(Color.Magenta)
                    .height(56.dp)
            )
        }
    }
}
