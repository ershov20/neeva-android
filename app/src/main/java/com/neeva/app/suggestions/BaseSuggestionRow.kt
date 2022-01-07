package com.neeva.app.suggestions

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
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.storage.Favicon
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
    faviconData: Favicon? = null,
    imageURL: String? = null,
    drawableID: Int? = null,
    drawableTint: Color? = null,
    mainContent: @Composable (modifier: Modifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = onTapRowContentDescription
            ) {
                onTapRow.invoke()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
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
                    faviconData,
                    modifier = iconModifier
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        mainContent(Modifier.weight(1.0f))

        if (onTapEdit != null) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_north_west_24),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        onClickLabel = stringResource(R.string.edit_content_description)
                    ) {
                        onTapEdit()
                    },
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
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
            faviconData = null
        ) {
            Box(modifier = it.background(Color.Magenta).height(56.dp))
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
            faviconData = null
        ) {
            Box(modifier = it.background(Color.Magenta).height(56.dp))
        }
    }
}
