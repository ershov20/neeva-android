package com.neeva.app.card

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.Dispatchers
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.browsing.TabInfo
import com.neeva.app.previewDispatchers
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.widgets.FaviconView
import kotlinx.coroutines.withContext

@Composable
fun TabCard(
    tabInfo: TabInfo,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    faviconCache: FaviconCache,
    screenshotProvider: (id: String) -> Bitmap?
) {
    TabCard(
        tabInfo = tabInfo,
        onSelect = onSelect,
        onClose = onClose,
        faviconCache = faviconCache,
        screenshotProvider = screenshotProvider,
        dispatchers = LocalEnvironment.current.dispatchers
    )
}

@Composable
fun TabCard(
    tabInfo: TabInfo,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    faviconCache: FaviconCache,
    screenshotProvider: (id: String) -> Bitmap?,
    dispatchers: Dispatchers
) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(tabInfo.url)

    LaunchedEffect(key1 = tabInfo.id) {
        withContext(dispatchers.io) {
            thumbnail = screenshotProvider(tabInfo.id)
        }
    }

    Surface {
        Column(
            modifier = Modifier
                .padding(Dimensions.PADDING_SMALL)
                .clickable { onSelect() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(200.dp),
                border = if (tabInfo.isSelected) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                }
            ) {
                Box {
                    Image(
                        bitmap = thumbnail?.asImageBitmap() ?: ImageBitmap(1, 1),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onClose() }
                            .padding(Dimensions.PADDING_TINY)
                            .background(Color.LightGray, shape = CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = stringResource(R.string.close),
                            contentScale = ContentScale.Inside,
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = Dimensions.PADDING_SMALL)
            ) {
                Box(modifier = Modifier.padding(end = Dimensions.PADDING_SMALL)) {
                    FaviconView(faviconBitmap)
                }
                Text(
                    text = tabInfo.title ?: tabInfo.url?.toString() ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = Dimensions.PADDING_SMALL)
                        .weight(1.0f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview("Long title, LTR, 1x scale", locale = "en")
@Preview("Long title, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Long title, RTL, 1x scale", locale = "he")
@Composable
private fun TabCardPreview_LongString() {
    LightDarkPreviewContainer {
        val title = stringResource(id = R.string.debug_long_string_primary)

        TabCard(
            tabInfo = TabInfo(
                id = "unimportant",
                url = Uri.parse("https://www.reddit.com"),
                title = title,
                isSelected = false
            ),
            onSelect = {},
            onClose = {},
            faviconCache = mockFaviconCache,
            screenshotProvider = { null },
            dispatchers = previewDispatchers
        )
    }
}

@Preview("Short title, LTR, 1x scale", locale = "en")
@Preview("Short title, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Short title, RTL, 1x scale", locale = "he")
@Composable
private fun TabCardPreview_ShortTitleSelected() {
    LightDarkPreviewContainer {
        val title = "short"
        TabCard(
            tabInfo = TabInfo(
                id = "unimportant",
                url = Uri.parse("https://www.reddit.com"),
                title = title,
                isSelected = true
            ),
            onSelect = {},
            onClose = {},
            faviconCache = mockFaviconCache,
            screenshotProvider = { null },
            dispatchers = previewDispatchers
        )
    }
}
