package com.neeva.app.spaces

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.entities.Space
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.widgets.overlay.OverlaySheet
import com.neeva.app.widgets.overlay.OverlaySheetConfig

@Composable
fun AddToSpaceSheet(
    webLayerModel: WebLayerModel,
    spaceModifier: SpaceModifier
) {
    val appNavModel = LocalAppNavModel.current
    val spaceStore = LocalEnvironment.current.spaceStore

    val browserWrapper by webLayerModel.browserWrapperFlow.collectAsState()
    val activeTabModel = browserWrapper.activeTabModel

    OverlaySheet(config = OverlaySheetConfig.spaces) {
        AddToSpaceUI(
            activeTabModel,
            spaceStore,
            spaceModifier,
            onDismiss = appNavModel::showBrowser
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddToSpaceUI(
    activeTabModel: ActiveTabModel,
    spaceStore: SpaceStore,
    spaceModifier: SpaceModifier,
    onDismiss: () -> Unit
) {
    val spaces: List<Space> by spaceStore.editableSpacesFlow.collectAsState()

    LazyColumn {
        stickyHeader {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TODO(kobec): might be wrong font style
                Text(
                    "Save to Spaces",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_close_24),
                    contentDescription = "Close Settings",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier
                        .size(48.dp, 48.dp)
                        .clickable { onDismiss() },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        items(spaces) {
            SpaceRow(space = it, activeTabModel) {
                spaceModifier.addOrRemoveCurrentTabToSpace(it)
            }
        }
    }
}

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
fun SpaceRow(space: Space, activeTabModel: ActiveTabModel? = null, onClick: () -> Unit) {
    val thumbnail: ImageBitmap? by getThumbnailAsync(uri = space.thumbnail)

    Row(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(horizontal = 16.dp)
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnail == null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ColorPalette.Brand.PolarVariant)
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.spaces),
                        contentDescription = "Url in space indicator",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(24.dp, 24.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            } else {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .size(48.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = space.name,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (space.isPublic) {
                Image(
                    painter = painterResource(id = R.drawable.ic_baseline_link_24),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(24.dp, 24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline)
                )
            }
            if (space.isShared) {
                Image(
                    painter = painterResource(id = R.drawable.ic_baseline_people_24),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(24.dp, 24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        val spaceHasUrl: Boolean by remember {
            mutableStateOf(space.contentURLs?.contains(activeTabModel?.urlFlow?.value) == true)
        }
        Image(
            painter = painterResource(
                if (spaceHasUrl) {
                    R.drawable.ic_baseline_bookmark_24
                } else {
                    R.drawable.ic_baseline_bookmark_border_24
                }
            ),
            contentDescription = if (spaceHasUrl) {
                stringResource(id = R.string.space_contains_page)
            } else {
                stringResource(id = R.string.space_not_contain_page)
            },
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(24.dp, 24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline)
        )
    }
}
