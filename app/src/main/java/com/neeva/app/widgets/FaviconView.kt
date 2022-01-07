package com.neeva.app.widgets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun FaviconView(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    bordered: Boolean = true
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .then(
                if (bordered) {
                    Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            ),
        Alignment.Center
    ) {
        Image(
            bitmap = bitmap?.asImageBitmap() ?: ImageBitmap.imageResource(id = R.drawable.globe),
            contentDescription = "favicon",
            modifier = Modifier
                .size(16.dp)
                .padding(2.dp),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Preview(group = "Globe favicon")
@Composable
fun FaviconView_Globe_Bordered() {
    NeevaTheme {
        FaviconView(bitmap = null, bordered = true)
    }
}

@Preview(group = "Globe favicon")
@Composable
fun FaviconView_Globe_NoBorder() {
    NeevaTheme {
        FaviconView(bitmap = null, bordered = false)
    }
}

@Preview(group = "Blank favicon")
@Composable
fun FaviconView_Blank_Bordered() {
    NeevaTheme {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.MAGENTA)
        FaviconView(bitmap, bordered = true)
    }
}

@Preview(group = "Blank favicon")
@Composable
fun FaviconView_Blank_NoBorder() {
    NeevaTheme {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.MAGENTA)
        FaviconView(bitmap, bordered = false)
    }
}
