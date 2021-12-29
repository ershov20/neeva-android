package com.neeva.app.spaces

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.R
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.storage.Space
import com.neeva.app.storage.SpaceStore
import com.neeva.app.widgets.OverlaySheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddToSpaceSheet(appNavModel: AppNavModel, selectedTabModel: SelectedTabModel) {
    OverlaySheet(appNavModel = appNavModel, visibleState = AppNavState.ADD_TO_SPACE) {
        AddToSpaceUI(
            selectedTabModel,
            onDismiss = { appNavModel.showBrowser() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddToSpaceUI(
    selectedTabModel: SelectedTabModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val spaces: List<Space> by SpaceStore.shared.allSpacesFlow.collectAsState()

    LazyColumn {
        stickyHeader {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Save to Spaces",
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.onPrimary,
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
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
                )
            }
        }

        items(spaces) {
            val title = selectedTabModel.titleFlow.collectAsState()

            SpaceRow(space = it, selectedTabModel) {
                scope.launch {
                    it.addOrRemove(
                        selectedTabModel.urlFlow.value,
                        title = title.value
                    )
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun SpaceRow(space: Space, selectedTabModel: SelectedTabModel? = null, onClick: () -> Unit) {
    val thumbnail: ImageBitmap = space.thumbnailAsBitmap().asImageBitmap()
    Row(modifier = Modifier
        .clickable {
            onClick()
        }
        .padding(horizontal = 16.dp)
        .padding(vertical = 4.dp)
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = thumbnail,
            contentDescription = "Space Thumbnail",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = space.name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onPrimary,
            maxLines = 1,
        )
        if (space.isPublic) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_link_24),
                contentDescription = "Url in space indicator",
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(48.dp, 48.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
        }
        if (space.isShared) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_people_24),
                contentDescription = "Url in space indicator",
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(48.dp, 48.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        if (selectedTabModel != null) {
            val spaceHasUrl: Boolean by remember {
                mutableStateOf(space.contentURLs?.contains(selectedTabModel.urlFlow.value) == true)
            }
            Image(
                imageVector = ImageVector.vectorResource(
                    if (spaceHasUrl) {
                        R.drawable.ic_baseline_bookmark_24
                    } else {
                        R.drawable.ic_baseline_bookmark_border_24
                    }
                ),
                contentDescription = "Url in space indicator",
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(48.dp, 48.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
        }
    }
}


