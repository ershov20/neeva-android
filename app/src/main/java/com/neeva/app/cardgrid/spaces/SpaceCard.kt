package com.neeva.app.cardgrid.spaces

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.neeva.app.Dispatchers
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.cardgrid.Card
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.createCheckerboardBitmap
import com.neeva.app.ui.createSingleColorBitmap
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import java.io.FileInputStream
import kotlinx.coroutines.withContext

private const val MAX_SUBITEMS_TO_SHOW = 4

/**
 * Instead of showing a fourth thumbnail, UX mocks indicate that we should show how many items are
 * remaining as a "+2" or similar number.
 */
fun shouldShowAdditionalItemCount(index: Int, numItems: Int): Boolean {
    return numItems > MAX_SUBITEMS_TO_SHOW && index == MAX_SUBITEMS_TO_SHOW - 1
}

data class SpaceItemThumbnails(
    val bitmaps: List<Bitmap?> = listOf(null, null, null, null),
    val numItems: Int = 0
)

@Composable
fun spaceThumbnailState(
    spaceId: String,
    itemProvider: suspend (spaceId: String) -> List<SpaceItem>,
    dispatchers: Dispatchers
): State<SpaceItemThumbnails> {
    val context = LocalContext.current.applicationContext
    val spaceStoreState = LocalEnvironment.current.spaceStore.stateFlow.collectAsState()

    // By keying this on [spaceId], we can avoid recompositions until the spaceId changes.
    return produceState(
        initialValue = SpaceItemThumbnails(),
        key1 = spaceId,
        key2 = spaceStoreState.value
    ) {
        val bitmaps = mutableListOf<Bitmap?>()
        val spaceItems = itemProvider(spaceId)
        for (i in 0 until MAX_SUBITEMS_TO_SHOW) {
            val itemBitmap = withContext(dispatchers.io) {
                spaceItems.getOrNull(i)
                    ?.thumbnail
                    ?.takeUnless {
                        // If we're going to be displaying a number instead of the bitmap, don't
                        // waste cycles loading it.
                        shouldShowAdditionalItemCount(i, spaceItems.size)
                    }
                    ?.let {
                        // if it is a file just load it, if not, fetch the Bitmap.
                        if (it.scheme == "file") {
                            BitmapIO.loadBitmap(it) { file ->
                                FileInputStream(file)
                            }
                        } else {
                            val loader = ImageLoader(context)
                            val request = ImageRequest.Builder(context).data(it.toString()).build()
                            loader.execute(request).drawable?.toBitmap()
                        }
                    }
            }
            bitmaps.add(itemBitmap)
        }

        value = SpaceItemThumbnails(bitmaps = bitmaps, numItems = spaceItems.size)
    }
}

@Composable
fun SpaceCard(
    spaceId: String,
    spaceName: String,
    isSpacePublic: Boolean,
    onSelect: () -> Unit,
    itemProvider: suspend (spaceId: String) -> List<SpaceItem>,
    dispatchers: Dispatchers = LocalEnvironment.current.dispatchers
) {
    val spaceItemThumbnails by spaceThumbnailState(
        spaceId,
        itemProvider,
        dispatchers
    )

    SpaceCard(
        spaceName = spaceName,
        isSpacePublic = isSpacePublic,
        onSelect = onSelect,
        spaceItemThumbnails = spaceItemThumbnails
    )
}

@Composable
fun SpaceCard(
    spaceName: String,
    isSpacePublic: Boolean,
    onSelect: () -> Unit,
    spaceItemThumbnails: SpaceItemThumbnails
) {
    val lockComposable = @Composable {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(Dimensions.SIZE_ICON)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
            )
        }
    }

    Card(
        label = spaceName,
        onSelect = onSelect,
        labelEndComposable = lockComposable.takeIf { !isSpacePublic }
    ) {
        Box {
            Column(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val thumbnailModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()

                    SpaceSubItem(
                        bitmap = spaceItemThumbnails.bitmaps.getOrNull(0),
                        corner = Corner.TOP_START,
                        modifier = thumbnailModifier,
                        index = 0,
                        numItems = spaceItemThumbnails.numItems
                    )

                    Spacer(modifier = Modifier.padding(2.dp))

                    SpaceSubItem(
                        bitmap = spaceItemThumbnails.bitmaps.getOrNull(1),
                        corner = Corner.TOP_END,
                        modifier = thumbnailModifier,
                        index = 1,
                        numItems = spaceItemThumbnails.numItems
                    )
                }

                Spacer(modifier = Modifier.padding(2.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val thumbnailModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()

                    SpaceSubItem(
                        bitmap = spaceItemThumbnails.bitmaps.getOrNull(2),
                        corner = Corner.BOTTOM_START,
                        modifier = thumbnailModifier,
                        index = 2,
                        numItems = spaceItemThumbnails.numItems
                    )

                    Spacer(modifier = Modifier.padding(2.dp))
                    SpaceSubItem(
                        bitmap = spaceItemThumbnails.bitmaps.getOrNull(3),
                        corner = Corner.BOTTOM_END,
                        modifier = thumbnailModifier,
                        index = 3,
                        numItems = spaceItemThumbnails.numItems
                    )
                }
            }
        }
    }
}

enum class Corner {
    TOP_START, TOP_END, BOTTOM_START, BOTTOM_END
}

@Composable
fun SpaceSubItem(
    bitmap: Bitmap?,
    corner: Corner,
    index: Int,
    numItems: Int,
    modifier: Modifier = Modifier
) {
    val outerRadius = Dimensions.RADIUS_MEDIUM
    val innerRadius = outerRadius / 4

    val topStartRadius = if (corner == Corner.TOP_START) outerRadius else innerRadius
    val topEndRadius = if (corner == Corner.TOP_END) outerRadius else innerRadius
    val bottomStartRadius = if (corner == Corner.BOTTOM_START) outerRadius else innerRadius
    val bottomEndRadius = if (corner == Corner.BOTTOM_END) outerRadius else innerRadius

    val tonalElevation = when {
        bitmap != null -> 1.dp
        shouldShowAdditionalItemCount(index, numItems) -> 0.dp
        index < numItems -> 1.dp
        else -> 0.dp
    }

    Surface(
        tonalElevation = tonalElevation,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(
            topStart = topStartRadius,
            topEnd = topEndRadius,
            bottomStart = bottomStartRadius,
            bottomEnd = bottomEndRadius
        ),
        modifier = modifier
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }

            shouldShowAdditionalItemCount(index, numItems) -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Text(
                        stringResource(
                            R.string.space_additional_count,
                            numItems - (MAX_SUBITEMS_TO_SHOW - 1)
                        )
                    )
                }
            }

            index < numItems -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorPalette.Brand.PolarVariant)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bookmarks_black_24),
                        contentDescription = null,
                        modifier = Modifier.padding(Dimensions.PADDING_SMALL),
                        tint = Color.White
                    )
                }
            }

            else -> {}
        }
    }
}

@Preview("Long title, LTR, 1x scale", locale = "en")
@Preview("Long title, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Long title, RTL, 1x scale", locale = "he")
@Composable
internal fun SpaceCardPreview_LongString() {
    LightDarkPreviewContainer {
        SpaceCard(
            spaceName = stringResource(R.string.debug_long_string_primary),
            isSpacePublic = true,
            onSelect = {},
            spaceItemThumbnails = SpaceItemThumbnails(
                bitmaps = listOf(
                    createCheckerboardBitmap(false),
                    null
                ),
                numItems = 2
            )
        )
    }
}

@Preview("Short title, LTR, 1x scale", locale = "en")
@Preview("Short title, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Short title, RTL, 1x scale", locale = "he")
@Composable
internal fun TabCardPreview_ShortTitleSelected() {
    LightDarkPreviewContainer {
        SpaceCard(
            spaceName = stringResource(id = R.string.debug_short_action),
            isSpacePublic = false,
            onSelect = {},
            spaceItemThumbnails = SpaceItemThumbnails(
                bitmaps = listOf(
                    createSingleColorBitmap(false, Color.Blue.toArgb()),
                    null,
                    createSingleColorBitmap(false, Color.Red.toArgb()),
                    null
                ),
                numItems = 5
            )
        )
    }
}
