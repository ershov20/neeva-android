package com.neeva.app.spaces

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.settings.sharedComposables.subcomponents.PictureUrlPainter
import com.neeva.app.storage.entities.SpaceEntityType
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions

@Composable
fun SpaceItemDetail(
    spaceItem: SpaceItem,
    showDescriptions: Boolean = false
) {
    val appNavModel = LocalAppNavModel.current
    val neevaConstants = LocalEnvironment.current.neevaConstants
    val isRegularWebItem = !spaceItem.url?.toString().isNullOrEmpty()

    Surface(
        if (isRegularWebItem) {
            Modifier.clickable {
                spaceItem.url?.let {
                    if (it.toString().startsWith(neevaConstants.appSpacesURL)) {
                        appNavModel.showSpaceDetail(it.pathSegments.last())
                    } else {
                        appNavModel.openUrl(it)
                    }
                }
            }
        } else {
            Modifier
        }
    ) {
        Column {
            if (spaceItem.itemEntityType == SpaceEntityType.IMAGE) {
                SpaceItemDetailImageContent(spaceItem = spaceItem)
            } else {
                SpaceItemDetailMainContent(
                    thumbnailUri = spaceItem.thumbnail,
                    isRegularWebItem = isRegularWebItem,
                    content = {
                        SpaceItemDetailTextContent(
                            spaceItem = spaceItem,
                            showDescriptions = showDescriptions
                        )
                    }
                )
            }

            val shouldShowDescriptionsForEntity =
                !spaceItem.snippet.isNullOrEmpty() && showDescriptions &&
                    spaceItem.itemEntityType != SpaceEntityType.IMAGE
            if (shouldShowDescriptionsForEntity) {
                BaseRowLayout(
                    backgroundColor =
                    if (isRegularWebItem) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    mainContent = {
                        Text(
                            text = spaceItem.snippet ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = Int.MAX_VALUE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.PADDING_MEDIUM)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ColumnScope.SpaceItemDetailImageContent(
    spaceItem: SpaceItem
) {
    PictureUrlPainter(pictureURI = spaceItem.url)?.let {
        Image(
            painter = it,
            contentScale = ContentScale.Fit,
            contentDescription = null,
            modifier = Modifier
                .heightIn(max = 300.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
    spaceItem.title?.let {
        BaseRowLayout(
            backgroundColor = MaterialTheme.colorScheme.surface,
            mainContent = {
                Text(
                    text = spaceItem.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.PADDING_MEDIUM)
                )
            }
        )
    }
}

@Composable
fun SpaceItemDetailTextContent(
    spaceItem: SpaceItem,
    showDescriptions: Boolean = false
) {
    val isRegularWebItem = !spaceItem.url?.toString().isNullOrEmpty()
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

        SpaceEntityInfo(spaceItem = spaceItem)

        if (!spaceItem.snippet.isNullOrEmpty() && !showDescriptions) {
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
        } else if (isRegularWebItem) {
            Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))
        }
    }
}

@Composable
fun SpaceItemDetailMainContent(
    content: @Composable () -> Unit,
    thumbnailUri: Uri?,
    isRegularWebItem: Boolean
) {
    val painter = PictureUrlPainter(pictureURI = thumbnailUri)

    if (isRegularWebItem) {
        if (painter == null || thumbnailUri.toString().isEmpty()) {
            BaseRowLayout(
                backgroundColor = MaterialTheme.colorScheme.surface,
                mainContent = content
            )
        } else {
            BaseRowLayout(
                backgroundColor = MaterialTheme.colorScheme.surface,
                startComposable = {
                    Image(
                        painter = painter,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(vertical = Dimensions.PADDING_LARGE)
                            .size(72.dp)
                            .clip(RoundedCornerShape(Dimensions.RADIUS_MEDIUM))
                    )
                },
                verticalAlignment = Alignment.Top,
                mainContent = content
            )
        }
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
    OneBooleanPreviewContainer { showDescriptions ->
        SpaceItemDetail(
            SpaceItem(
                "asjdahjfad",
                "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                Uri.parse("https://example.com/path/to/other/pages"),
                "Facebook documents offer a treasure trove for Washington Post",
                "Facebook likes to portray itself as a social media giant under" +
                    " siege — locked in fierce competition with other companies",
                null
            ),
            showDescriptions = showDescriptions
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
            ),
            showDescriptions = false
        )
    }
}
