package com.neeva.app.cardgrid.tabs

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.browsing.TabInfo
import com.neeva.app.cardgrid.Card
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.createCheckerboardBitmap
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.FaviconView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun thumbnailState(
    id: String,
    screenshotProvider: suspend (id: String) -> Bitmap?
): State<Bitmap?> {
    // By keying this on [uri], we can avoid recompositions until the tab ID changes.
    return produceState<Bitmap?>(initialValue = null, key1 = id) {
        withContext(Dispatchers.IO) { value = screenshotProvider(id) }
    }
}

@Composable
fun TabCard(
    tabInfo: TabInfo,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    faviconCache: FaviconCache,
    screenshotProvider: suspend (id: String) -> Bitmap?
) {
    val thumbnail by thumbnailState(tabInfo.id, screenshotProvider)
    val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(tabInfo.url)

    Card(
        label = tabInfo.title ?: tabInfo.url?.toString() ?: "",
        onSelect = onSelect,
        labelStartComposable = {
            FaviconView(
                bitmap = faviconBitmap,
                drawContainer = false
            )
        },
        modifier = Modifier.semantics { testTag = "TabCard" }
    ) {
        Surface(
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(Dimensions.RADIUS_MEDIUM),
            border = if (tabInfo.isSelected) {
                BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            modifier = if (tabInfo.isSelected) {
                Modifier.semantics { testTag = "SelectedTabCard" }
            } else {
                Modifier
            }
        ) {
            Box {
                Image(
                    bitmap = thumbnail?.asImageBitmap() ?: ImageBitmap(1, 1),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
            screenshotProvider = { createCheckerboardBitmap(false) }
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
            screenshotProvider = { createCheckerboardBitmap(false) }
        )
    }
}
