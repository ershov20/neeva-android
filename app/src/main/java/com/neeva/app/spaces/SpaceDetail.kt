package com.neeva.app.spaces

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun SpaceDetail() {
    val spaceStore = LocalEnvironment.current.spaceStore
    val spaces = spaceStore.allSpacesFlow.collectAsState()
    val spaceID = spaceStore.detailedSpaceIDFlow.collectAsState()
    val spaceStoreState = spaceStore.stateFlow.collectAsState()
    val fetchedSpace = spaceStore.fetchedSpaceFlow.collectAsState(initial = null)
    val space = remember(spaceID, spaceStoreState, fetchedSpace.value) {
        derivedStateOf {
            spaces.value.find { it.id == spaceID.value } ?: fetchedSpace.value
        }
    }
    val content = getSpaceContentsAsync(
        fetchedSpace = fetchedSpace.value,
        spaceStoreState = spaceStoreState.value,
        spaceID = spaceID.value
    )
    val sharedPrefs = LocalEnvironment.current.sharedPreferencesModel
    val showDescriptions = remember {
        mutableStateOf(
            sharedPrefs.getValue(
                SharedPrefFolder.Spaces,
                SharedPrefFolder.Spaces.ShowDescriptionsPreferenceKey,
                defaultValue = false
            )
        )
    }

    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        stickyHeader {
            SpaceDetailToolbar(
                lazyListState = state,
                space = space.value,
                showDescriptions = showDescriptions.value
            ) {
                sharedPrefs.setValue(
                    SharedPrefFolder.Spaces,
                    SharedPrefFolder.Spaces.ShowDescriptionsPreferenceKey,
                    !showDescriptions.value
                )
                showDescriptions.value = !showDescriptions.value
            }
        }

        space.value?.let {
            item {
                SpaceHeader(space = it)
            }
        }

        content.value?.let { content ->
            items(content, key = { it.id }) { spaceItem ->
                val canEdit = remember(space) {
                    space.value?.userACL == SpaceACLLevel.Edit ||
                        space.value?.userACL == SpaceACLLevel.Owner
                }
                val dismissDirectionSet = remember(canEdit) {
                    if (canEdit) {
                        setOf(DismissDirection.EndToStart)
                    } else {
                        emptySet()
                    }
                }
                val dismissState = rememberDismissState(
                    confirmStateChange = {
                        if (it == DismissValue.DismissedToStart) {
                            spaceStore.removeFromSpace(spaceItem)
                            return@rememberDismissState true
                        }
                        false
                    }
                )
                SwipeToDismiss(
                    state = dismissState,
                    directions = dismissDirectionSet,
                    background = {
                        val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                        // TODO Enable StartToEnd Edit.
                        val color by animateColorAsState(
                            when {
                                !canEdit -> MaterialTheme.colorScheme.background
                                direction == DismissDirection.EndToStart -> ColorPalette.Brand.Red
                                direction == DismissDirection.StartToEnd -> ColorPalette.Brand.Blue
                                else -> MaterialTheme.colorScheme.background
                            }
                        )
                        val alignment = when (direction) {
                            DismissDirection.StartToEnd -> Alignment.CenterStart
                            DismissDirection.EndToStart -> Alignment.CenterEnd
                        }
                        val icon = when (direction) {
                            DismissDirection.StartToEnd -> Icons.Default.Edit
                            DismissDirection.EndToStart -> Icons.Default.Delete
                        }
                        val scale by animateFloatAsState(
                            if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f
                        )

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = Dimensions.PADDING_LARGE),
                            contentAlignment = alignment
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    }
                ) {
                    SpaceItemDetail(
                        spaceItem = spaceItem,
                        showDescriptions = showDescriptions.value
                    )
                }
            }
        }
    }
}

@Composable
fun SpaceDetailToolbar(
    lazyListState: LazyListState,
    space: Space?,
    showDescriptions: Boolean,
    toggleShowDescriptions: () -> Unit
) {
    val neevaConstants = LocalEnvironment.current.neevaConstants
    val url = space?.url(neevaConstants = neevaConstants)
    SmallTopAppBar(
        navigationIcon = {
            val appNavModel = LocalAppNavModel.current
            RowActionIconButton(
                iconParams = RowActionIconParams(
                    onTapAction = { appNavModel.popBackStack() },
                    actionType = RowActionIconParams.ActionType.BACK
                )
            )
        },
        title = {
            val isVisible by remember {
                derivedStateOf {
                    lazyListState.firstVisibleItemIndex > 0
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Text(
                        text = space?.name ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = space?.ownerName ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }
            val appNavModel = LocalAppNavModel.current
            IconButton(
                onClick = toggleShowDescriptions
            ) {
                Icon(
                    painter = painterResource(
                        if (showDescriptions) {
                            R.drawable.ic_hide_descriptions
                        } else {
                            R.drawable.ic_show_descriptions
                        }
                    ),
                    contentDescription = stringResource(
                        id = R.string.space_detail_show_descriptions
                    ),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            RowActionIconButton(
                iconParams = RowActionIconParams(
                    onTapAction = { space?.let { appNavModel.shareSpace(it) } },
                    actionType = RowActionIconParams.ActionType.SHARE
                )
            )

            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.more),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            url?.let { spaceUrl ->
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.defaultMinSize(minWidth = 250.dp)
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(id = R.string.spaces_edit),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(id = R.string.space_edit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = { appNavModel.openUrl(spaceUrl) }
                    )
                }
            }
        },
    )
}

/** Returns a [State] that can be used in a Composable for contents of a Space. */
@Composable
fun getSpaceContentsAsync(
    fetchedSpace: Space?,
    spaceStoreState: SpaceStore.State,
    spaceID: String?
): State<List<SpaceItem>?> {
    val spaceStore = LocalEnvironment.current.spaceStore
    return produceState<List<SpaceItem>?>(
        initialValue = null,
        spaceID,
        spaceStoreState,
        fetchedSpace
    ) {
        val id = spaceID ?: return@produceState
        value = spaceStore.contentDataForSpace(spaceID = id)
    }
}

@Preview
@Composable
fun SpaceDetailToolbarPreview() {
    OneBooleanPreviewContainer { showDescriptions ->
        SpaceDetailToolbar(
            rememberLazyListState(),
            space = null,
            showDescriptions = showDescriptions,
            toggleShowDescriptions = {}
        )
    }
}
