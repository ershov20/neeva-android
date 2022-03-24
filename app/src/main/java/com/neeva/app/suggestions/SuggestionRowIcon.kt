package com.neeva.app.suggestions

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.FaviconView

data class SuggestionRowIconParams(
    val imageURL: String? = null,

    val drawableID: Int? = null,
    val drawableTint: Color = Color.Unspecified,

    val faviconBitmap: Bitmap? = null
)
@Composable
fun SuggestionRowIcon(params: SuggestionRowIconParams) {
    when {
        !params.imageURL.isNullOrBlank() -> {
            Icon(
                painter = rememberImagePainter(
                    data = params.imageURL,
                    builder = { crossfade(true) }
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.padding(Dimensions.PADDING_SMALL).clip(RoundedCornerShape(4.dp))
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
