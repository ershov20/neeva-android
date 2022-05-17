package com.neeva.app.spaces

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.subcomponents.PictureUrlPainter
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.UriDisplayView

@Composable
fun SpaceItemDetail(
    spaceItem: SpaceItem
) {
    val content = @Composable {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = spaceItem.title ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.PADDING_MEDIUM)
            )
            spaceItem.url?.let { url ->
                UriDisplayView(uri = url)
            }
            if (!spaceItem.snippet.isNullOrEmpty()) {
                Text(
                    text = spaceItem.snippet,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.PADDING_MEDIUM)
                )
            } else if (spaceItem.url != null) {
                Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))
            }
        }
    }
    if (spaceItem.url != null) {
        BaseRowLayout(
            backgroundColor = MaterialTheme.colorScheme.background,
            startComposable = {
                val painter = PictureUrlPainter(pictureURI = spaceItem.thumbnail)
                if (painter == null) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bookmarks_black_24),
                        contentDescription = null,
                        modifier = Modifier
                            .background(
                                color = ColorPalette.Brand.Maya,
                                shape = RoundedCornerShape(Dimensions.RADIUS_TINY)
                            ).padding(Dimensions.PADDING_SMALL),
                        tint = Color.White
                    )
                } else {
                    Image(
                        painter = painter!!,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(Dimensions.RADIUS_MEDIUM))
                    )
                }
            },
            mainContent = content
        )
    } else {
        BaseRowLayout(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            mainContent = content
        )
    }
}

@Preview
@Composable
fun SpaceItemPreview() {
    OneBooleanPreviewContainer { isDescriptionEmpty ->
        SpaceItemDetail(
            SpaceItem(
                "asjdahjfad",
                "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                Uri.parse("https://example.com/path/to/other/pages"),
                "Facebook documents offer a treasure trove for Washington Post",
                if (isDescriptionEmpty) {
                    ""
                } else {
                    "Facebook likes to portray itself as a social media giant under" +
                        " siege — locked in fierce competition with other companies"
                },
                null
            )
        )
    }
}

@Preview
@Composable
fun SpaceSectionHeaderPreview() {
    OneBooleanPreviewContainer { isDescriptionEmpty ->
        SpaceItemDetail(
            SpaceItem(
                "asjdahjfad",
                "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                null,
                "Facebook papers notes",
                if (isDescriptionEmpty) {
                    ""
                } else {
                    "Facebook likes to portray itself as a social media giant under" +
                        " siege — locked in fierce competition with other companies"
                },
                null
            )
        )
    }
}
