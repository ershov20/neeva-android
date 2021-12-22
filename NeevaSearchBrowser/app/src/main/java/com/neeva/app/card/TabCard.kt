package com.neeva.app.card

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.browsing.TabInfo
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@Composable
fun TabCard(
    tab: TabInfo,
    faviconData: Bitmap?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(10.dp)) {
        Box(
            modifier = Modifier.then(
                if (tab.isSelected) {
                    Modifier.border(3.dp, Color.Blue, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
        ) {
            Image(
                painter = rememberImagePainter(
                    data = tab.thumbnailUri,
                    builder = { crossfade(true) }
                ),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .shadow(2.dp, shape = RoundedCornerShape(12.dp))
                    .clickable { onSelect() }
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onClose() }
                    .padding(6.dp)
                    .background(Color.LightGray, shape = CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_close_24),
                    contentDescription = "Close tab",
                    contentScale = ContentScale.Inside,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                FaviconView(faviconData)
            }
            Text(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1.0f),
                text = tab.title ?: tab.url.toString(),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview("Not selected, 1x scale")
@Preview("Not selected, 2x scale", fontScale = 2.0f)
@Composable
fun TabCard_PreviewIsNotSelected() {
    NeevaTheme {
        TabCard(
            tab = TabInfo(
                id = "unimportant",
                thumbnailUri = null,
                url = Uri.parse("https://www.reddit.com"),
                title = "This is a long tab title that just keeps going and going and going and going",
                isSelected = false
            ),
            faviconData = null,
            onSelect = {},
            onClose = {}
        )
    }
}

@Preview("Selected, 1x scale")
@Preview("Selected, 2x scale", fontScale = 2.0f)
@Composable
fun TabCard_PreviewIsSelected() {
    NeevaTheme {
        TabCard(
            tab = TabInfo(
                id = "unimportant",
                thumbnailUri = null,
                url = Uri.parse("https://www.reddit.com"),
                title = "This is a long tab title that just keeps going and going and going and going",
                isSelected = true
            ),
            faviconData = null,
            onSelect = {},
            onClose = {}
        )
    }
}