package com.neeva.app.widgets

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

/**
 * Draws an icon representing a particular site.
 *
 * When determining what image to display, the [imageOverride] is prioritized over the [bitmap].  If
 * both are null, then a generic icon is displayed.
 */
@Composable
fun FaviconView(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    imageOverride: ImageVector? = null,
    bordered: Boolean = true,
    size: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (bordered) {
                    Modifier
                        .clip(RoundedCornerShape(Dimensions.RADIUS_SMALL))
                        .border(
                            1.dp, MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Dimensions.RADIUS_SMALL)
                        )
                        .padding(1.dp)
                } else {
                    Modifier.clip(RoundedCornerShape(Dimensions.RADIUS_LARGE))
                }
            ),
        Alignment.Center
    ) {
        when {
            imageOverride != null -> {
                Image(
                    imageVector = imageOverride,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(LocalContentColor.current)
                )
            }

            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }

            else -> {
                Image(
                    painter = painterResource(R.drawable.globe),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(LocalContentColor.current)
                )
            }
        }
    }
}

@Preview
@Composable
private fun FaviconViewPreviews() {
    TwoBooleanPreviewContainer { bordered, showBitmap ->
        val bitmap = if (showBitmap) {
            Uri.parse(NeevaConstants.appURL).toBitmap()
        } else {
            null
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground
            ) {
                FaviconView(
                    bitmap = bitmap,
                    bordered = bordered
                )
            }
        }
    }
}
