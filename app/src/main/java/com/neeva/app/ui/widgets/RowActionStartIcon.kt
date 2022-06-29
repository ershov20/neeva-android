package com.neeva.app.ui.widgets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.neeva.app.ui.theme.Dimensions

data class RowActionStartIconParams(
    val imageURL: String? = null,

    val drawableID: Int? = null,
    val drawableTint: Color = Color.Unspecified,

    val faviconBitmap: Bitmap? = null
)
@Composable
fun RowActionStartIcon(params: RowActionStartIconParams) {
    when {
        !params.imageURL.isNullOrBlank() -> {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = params.imageURL)
                        .apply(block = { crossfade(true) })
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(Dimensions.SIZE_ICON_INCLUDING_PADDING)
                    .clip(RoundedCornerShape(Dimensions.RADIUS_TINY))
            )
        }

        params.drawableID != null -> {
            Icon(
                painter = painterResource(params.drawableID),
                contentDescription = null,
                tint = params.drawableTint,
                modifier = Modifier.padding(Dimensions.PADDING_SMALL)
            )
        }

        else -> {
            FaviconView(bitmap = params.faviconBitmap)
        }
    }
}
