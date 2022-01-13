package com.neeva.app.card

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.browsing.TabInfo
import com.neeva.app.storage.Favicon
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@OptIn(ExperimentalCoilApi::class)
@Composable
fun TabCard(
    tab: TabInfo,
    faviconData: Favicon?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .clickable { onSelect() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                FaviconView(faviconData)
            }
            Text(
                text = tab.title ?: tab.url.toString(),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onPrimary,
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

        NeevaTheme(darkTheme = params.darkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colors.primary)) {
                TabCard(
                    tab = TabInfo(
                        id = "unimportant",
                        thumbnailUri = null,
                        url = Uri.parse("https://www.reddit.com"),
                        title = title,
                        isSelected = params.isSelected
                    ),
                    faviconData = null,
                    onSelect = {},
                    onClose = {}
                )
            }
        }
    }
}
