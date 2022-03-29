package com.neeva.app.spaces

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.entities.Space
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.createCheckerboardBitmap
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.Dimensions.PADDING_SMALL

/** Returns a [State] that can be used in a Composable for obtaining a Bitmap. */
@Composable
fun getThumbnailAsync(uri: Uri?): State<ImageBitmap?> {
    // By keying this on [uri], we can avoid recompositions until [uri] changes.  This avoids
    // infinite loops of recompositions that can be triggered via [Flow.collectAsState()].
    return produceState<ImageBitmap?>(initialValue = null, uri) {
        value = BitmapIO.loadBitmap(uri)?.asImageBitmap()
    }
}

@Composable
fun SpaceRow(
    space: Space,
    activeTabModel: ActiveTabModel,
    onClick: () -> Unit
) {
    val spacesWithURL by activeTabModel.spacesContainingCurrentUrlFlow.collectAsState()
    SpaceRow(space, spacesWithURL.contains(space.id), onClick)
}

@Composable
fun SpaceRow(
    space: Space,
    isCurrentUrlInSpace: Boolean? = null,
    onClick: () -> Unit
) {
    val thumbnail: ImageBitmap? by getThumbnailAsync(uri = space.thumbnail)
    SpaceRow(space, thumbnail, isCurrentUrlInSpace, onClick)
}

@Composable
fun SpaceRow(
    space: Space,
    thumbnail: ImageBitmap?,
    isCurrentUrlInSpace: Boolean? = null,
    onClick: () -> Unit
) {
    SpaceRow(
        spaceName = space.name,
        isSpacePublic = space.isPublic,
        thumbnail = thumbnail,
        isCurrentUrlInSpace = isCurrentUrlInSpace,
        onClick = onClick
    )
}

@Composable
fun SpaceRow(
    spaceName: String,
    isSpacePublic: Boolean,
    thumbnail: ImageBitmap?,
    isCurrentUrlInSpace: Boolean? = null,
    onClick: () -> Unit
) {
    val startComposable = @Composable {
        Surface(
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(Dimensions.RADIUS_SMALL)
        ) {
            if (thumbnail == null) {
                Icon(
                    painter = painterResource(id = R.drawable.spaces),
                    contentDescription = null,
                    modifier = Modifier.padding(PADDING_SMALL)
                )
            } else {
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Dimensions.SIZE_ICON_INCLUDING_PADDING)
                )
            }
        }
    }

    val showEndComposable = !isSpacePublic || isCurrentUrlInSpace != null
    val endComposable = @Composable {
        // Use Dimensions.SIZE_TOUCH_TARGET to try and ensure that it lines up properly when it's
        // displayed with other BaseRowLayouts.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isSpacePublic) {
                Box(
                    modifier = Modifier.size(Dimensions.SIZE_TOUCH_TARGET),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null
                    )
                }
            }
            if (isCurrentUrlInSpace != null) {
                Box(
                    modifier = Modifier.size(Dimensions.SIZE_TOUCH_TARGET),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isCurrentUrlInSpace == true) {
                                R.drawable.ic_baseline_bookmark_24
                            } else {
                                R.drawable.ic_baseline_bookmark_border_24
                            }
                        ),
                        contentDescription = if (isCurrentUrlInSpace == true) {
                            stringResource(id = R.string.space_contains_page)
                        } else {
                            stringResource(id = R.string.space_not_contain_page)
                        }
                    )
                }
            }
        }
    }

    BaseRowLayout(
        onTapRow = onClick,
        startComposable = startComposable,
        endComposable = endComposable.takeIf { showEndComposable }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = spaceName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun SpaceRowPreview_AddToSpaces() {
    TwoBooleanPreviewContainer { isSpacePublic, isThumbnailAvailable ->
        val thumbnail: ImageBitmap? = if (isThumbnailAvailable) {
            createCheckerboardBitmap(true).asImageBitmap()
        } else {
            null
        }
        val isCurrentUrlInSpace: Boolean? = null

        SpaceRow(
            spaceName = stringResource(id = R.string.debug_long_string_primary),
            isSpacePublic = isSpacePublic,
            thumbnail = thumbnail,
            isCurrentUrlInSpace = isCurrentUrlInSpace,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun SpaceRowPreview_ZeroQuery() {
    TwoBooleanPreviewContainer { isSpacePublic, isCurrentUrlInSpace ->
        val thumbnail: ImageBitmap? = null

        SpaceRow(
            spaceName = stringResource(id = R.string.debug_long_string_primary),
            isSpacePublic = isSpacePublic,
            thumbnail = thumbnail,
            isCurrentUrlInSpace = isCurrentUrlInSpace,
            onClick = {}
        )
    }
}
