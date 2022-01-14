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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.storage.Favicon.Companion.toBitmap
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun FaviconView(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
    size: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (bordered) {
                    Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .padding(2.dp),
        Alignment.Center
    ) {
        bitmap?.asImageBitmap()?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        } ?: run {
            Image(
                painter = painterResource(R.drawable.globe),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

@Preview(name = "Globe favicon, bordered")
@Composable
fun FaviconView_Globe_Bordered() {
    NeevaTheme {
        FaviconView(bitmap = null, bordered = true)
    }
}

@Preview(name = "Globe favicon, no border")
@Composable
fun FaviconView_Globe_NoBorder() {
    NeevaTheme {
        FaviconView(bitmap = null, bordered = false)
    }
}

@Preview(group = "Solid favicon, bordered")
@Composable
fun FaviconView_Blank_Bordered() {
    NeevaTheme {
        FaviconView(Uri.parse("https://www.neeva.com").toBitmap(), bordered = true)
    }
}

@Preview(group = "Solid favicon, no border")
@Composable
fun FaviconView_Blank_NoBorder() {
    NeevaTheme {
        FaviconView(Uri.parse("https://www.neeva.com").toBitmap(), bordered = false)
    }
}
