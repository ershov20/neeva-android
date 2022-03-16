package com.neeva.app.card

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.Dispatchers
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.browsing.TabInfo
import com.neeva.app.previewDispatchers
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme
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

    Column(
        modifier = Modifier
            .padding(10.dp)
            .clickable { onSelect() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.then(
                if (tabInfo.isSelected) {
                    Modifier.border(
                        3.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
        ) {
            Image(
                bitmap = thumbnail?.asImageBitmap() ?: ImageBitmap(1, 1),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .shadow(2.dp, shape = RoundedCornerShape(12.dp))
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
                    contentDescription = stringResource(id = R.string.close),
                    contentScale = ContentScale.Inside,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(modifier = Modifier.padding(end = 8.dp)) {
                FaviconView(faviconBitmap)
            }
            Text(
                text = tabInfo.title ?: tabInfo.url?.toString() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1.0f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

class TabCardPreviews : BooleanPreviewParameterProvider<TabCardPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val isSelected: Boolean,
        val useLongString: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isSelected = booleanArray[1],
        useLongString = booleanArray[2]
    )

    @Preview("1x scale", locale = "en")
    @Preview("2x scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x scale", locale = "he")
    @Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun TabCard_Preview(@PreviewParameter(TabCardPreviews::class) params: Params) {
        val title = if (params.useLongString) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "short"
        }

        NeevaTheme(useDarkTheme = params.darkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TabCard(
                    tabInfo = TabInfo(
                        id = "unimportant",
                        url = Uri.parse("https://www.reddit.com"),
                        title = title,
                        isSelected = params.isSelected
                    ),
                    onSelect = {},
                    onClose = {},
                    faviconCache = mockFaviconCache,
                    screenshotProvider = { null },
                    dispatchers = previewDispatchers
                )
            }
        }
    }
}
