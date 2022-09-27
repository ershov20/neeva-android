// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.LocalSpaceStore
import com.neeva.app.R
import com.neeva.app.overflowmenu.OverflowMenu
import com.neeva.app.overflowmenu.OverflowMenuData
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.ConfirmationAlertDialog
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.StackedText
import com.neeva.app.ui.widgets.menu.MenuAction
import com.neeva.app.ui.widgets.menu.MenuItem

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun SpaceDetail(spaceID: String?) {
    val spaceStore = LocalSpaceStore.current
    val spaces = spaceStore.allSpacesFlow.collectAsState()
    val spaceStoreState = spaceStore.stateFlow.collectAsState()
    val fetchedSpace = spaceStore.fetchedSpaceFlow.collectAsState(initial = null)
    val space = remember(spaceID, spaceStoreState, fetchedSpace.value) {
        derivedStateOf {
            spaces.value.find { it.id == spaceID } ?: fetchedSpace.value
        }
    }
    val content = getSpaceContentsAsync(
        fetchedSpace = fetchedSpace.value,
        spaceStoreState = spaceStoreState.value,
        spaceID = spaceID
    )
    val sharedPrefs = LocalSharedPreferencesModel.current
    val showDescriptions by SharedPrefFolder.App.SpacesShowDescriptionsPreferenceKey
        .getFlow(sharedPrefs)
        .collectAsState()

    val showRemoveSpaceConfirmationDialog = remember {
        mutableStateOf(false)
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
                showRemoveSpaceConfirmationDialog = showRemoveSpaceConfirmationDialog,
                showDescriptions = showDescriptions
            ) {
                SharedPrefFolder.App.SpacesShowDescriptionsPreferenceKey.set(
                    sharedPrefs,
                    !showDescriptions
                )
            }
        }

        space.value?.let {
            item {
                SpaceHeader(space = it)
            }
        }

        content.value?.let { content ->
            items(content, key = { it.id + it.title + it.snippet }) { spaceItem ->
                val appNavModel = LocalAppNavModel.current
                val canEdit = remember(space) {
                    space.value?.userACL == SpaceACLLevel.Edit ||
                        space.value?.userACL == SpaceACLLevel.Owner
                }
                val dismissDirectionSet = remember(canEdit) {
                    if (canEdit) {
                        setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd)
                    } else {
                        emptySet()
                    }
                }
                val dismissState = rememberDismissState(
                    confirmStateChange = {
                        if (it == DismissValue.DismissedToStart) {
                            spaceStore.removeFromSpace(spaceItem)
                            return@rememberDismissState true
                        } else if (it == DismissValue.DismissedToEnd) {
                            appNavModel.showEditSpaceDialog(
                                SpaceEditMode.EDITING_SPACE_ITEM,
                                spaceItem,
                                null
                            )
                        }
                        false
                    }
                )
                SwipeToDismiss(
                    state = dismissState,
                    directions = dismissDirectionSet,
                    background = {
                        val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
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
                        showDescriptions = showDescriptions
                    )
                }
            }
        }
    }
    if (showRemoveSpaceConfirmationDialog.value) {
        val isOwner = space.value?.userACL == SpaceACLLevel.Owner
        val actionLabel = stringResource(
            if (isOwner) {
                R.string.delete
            } else {
                R.string.unfollow
            }
        )

        val message = stringResource(
            if (isOwner) {
                R.string.space_delete_confirmation
            } else {
                R.string.space_unfollow_confirmation
            }
        )

        val appNavModel = LocalAppNavModel.current

        ConfirmationAlertDialog(
            title = actionLabel,
            message = message,
            onDismiss = { showRemoveSpaceConfirmationDialog.value = false },
            onConfirm = {
                showRemoveSpaceConfirmationDialog.value = false
                space.value?.id?.let { spaceStore.deleteOrUnfollowSpace(it) }
                appNavModel.popBackStack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailToolbar(
    lazyListState: LazyListState,
    space: Space?,
    showDescriptions: Boolean,
    showRemoveSpaceConfirmationDialog: MutableState<Boolean>,
    toggleShowDescriptions: () -> Unit
) {
    val neevaConstants = LocalNeevaConstants.current
    val url = space?.url(neevaConstants = neevaConstants)
    val canEdit = space?.userACL == SpaceACLLevel.Edit || space?.userACL == SpaceACLLevel.Owner
    val isTitleVisible by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }
    val appNavModel = LocalAppNavModel.current
    val showDescriptionsResourceId = if (showDescriptions) {
        R.drawable.ic_hide_descriptions
    } else {
        R.drawable.ic_show_descriptions
    }

    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = isTitleVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                StackedText(
                    primaryLabel = space?.name ?: "",
                    secondaryLabel = space?.ownerName?.takeIf { it.isNotBlank() },
                    primaryTextStyle = MaterialTheme.typography.titleSmall,
                    secondaryTextStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        navigationIcon = {
            RowActionIconButton(
                iconParams = RowActionIconParams(
                    onTapAction = { appNavModel.popBackStack() },
                    actionType = RowActionIconParams.ActionType.BACK
                )
            )
        },
        actions = {
            val onEditSpace = {
                appNavModel.showEditSpaceDialog(
                    SpaceEditMode.EDITING_SPACE,
                    null,
                    space
                )
            }
            val onAddToSpace = {
                appNavModel.showEditSpaceDialog(
                    SpaceEditMode.ADDING_SPACE_ITEM,
                    null,
                    space
                )
            }

            if (canEdit && !isTitleVisible) {
                RowActionIconButton(
                    iconParams = RowActionIconParams(
                        onTapAction = onAddToSpace,
                        actionType = RowActionIconParams.ActionType.ADD,
                        contentDescription = stringResource(R.string.space_add_space_item)
                    )
                )

                RowActionIconButton(
                    iconParams = RowActionIconParams(
                        onTapAction = onEditSpace,
                        actionType = RowActionIconParams.ActionType.EDIT,
                        contentDescription = stringResource(R.string.space_edit)
                    )
                )
            }

            if (!isTitleVisible) {
                IconButton(onClick = toggleShowDescriptions) {
                    Icon(
                        painter = painterResource(showDescriptionsResourceId),
                        contentDescription = stringResource(
                            id = R.string.space_detail_show_descriptions
                        ),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            RowActionIconButton(
                iconParams = RowActionIconParams(
                    onTapAction = {
                        space?.let {
                            if (it.userACL == SpaceACLLevel.Owner) {
                                appNavModel.showShareSpaceSheet(space.id)
                            } else {
                                appNavModel.shareSpace(it)
                            }
                        }
                    },
                    actionType = RowActionIconParams.ActionType.SHARE
                )
            )

            url?.let { spaceUrl ->
                val menuItems = mutableListOf<MenuItem>()
                val menuActions = mutableMapOf<Int, () -> Unit>()

                val isOwner = space.userACL == SpaceACLLevel.Owner
                val removeLabelId = if (isOwner) {
                    R.string.delete
                } else {
                    R.string.unfollow
                }

                if (!space.isDefaultSpace) {
                    menuItems.add(
                        MenuAction(
                            id = removeLabelId,
                            icon = Icons.Outlined.Delete
                        )
                    )
                    menuActions[removeLabelId] = {
                        showRemoveSpaceConfirmationDialog.value = true
                    }
                }

                menuItems.add(
                    MenuAction(
                        id = R.string.space_edit_on_web,
                        icon = Icons.Outlined.ExitToApp
                    )
                )
                menuActions[R.string.space_edit_on_web] =
                    { appNavModel.openUrlInNewTab(spaceUrl) }

                if (canEdit && isTitleVisible) {
                    menuItems.add(
                        MenuAction(
                            id = R.string.space_edit,
                            icon = Icons.Outlined.Edit
                        )
                    )
                    menuActions[R.string.space_edit] = onEditSpace

                    menuItems.add(
                        MenuAction(
                            id = R.string.space_add_space_item,
                            icon = Icons.Outlined.Add
                        )
                    )
                    menuActions[R.string.space_add_space_item] = onAddToSpace
                }

                if (isTitleVisible) {
                    menuItems.add(
                        MenuAction(
                            id = R.string.space_detail_show_descriptions,
                            imageResourceID = showDescriptionsResourceId
                        )
                    )
                    menuActions[R.string.space_detail_show_descriptions] = {
                        toggleShowDescriptions()
                    }
                }

                OverflowMenu(
                    overflowMenuData = OverflowMenuData(
                        additionalRowItems = menuItems,
                        showDefaultItems = false
                    ),
                    onMenuItem = { id -> menuActions[id]?.invoke() }
                )
            }
        }
    )
}

/** Returns a [State] that can be used in a Composable for contents of a Space. */
@Composable
fun getSpaceContentsAsync(
    fetchedSpace: Space?,
    spaceStoreState: SpaceStore.State,
    spaceID: String?
): State<List<SpaceItem>?> {
    val spaceStore = LocalSpaceStore.current
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

@LandscapePreviews
@PortraitPreviews
@Composable
fun SpaceDetailToolbarPreview() {
    OneBooleanPreviewContainer { showDescriptions ->
        val dialogState = remember { mutableStateOf(false) }
        SpaceDetailToolbar(
            rememberLazyListState(initialFirstVisibleItemIndex = 1),
            space = Space(
                id = "unused",
                name = "Space name",
                lastModifiedTs = "Last modified",
                thumbnail = null,
                resultCount = 5,
                isDefaultSpace = false,
                isShared = false,
                isPublic = false,
                ownerName = "Name of the owner",
                userACL = SpaceACLLevel.Owner
            ),
            showDescriptions = showDescriptions,
            showRemoveSpaceConfirmationDialog = dialogState,
            toggleShowDescriptions = {}
        )
    }
}

@LandscapePreviews
@PortraitPreviews
@Composable
fun SpaceDetailToolbarPreviewNoOwner() {
    OneBooleanPreviewContainer { showDescriptions ->
        val dialogState = remember { mutableStateOf(false) }
        SpaceDetailToolbar(
            rememberLazyListState(initialFirstVisibleItemIndex = 1),
            space = Space(
                id = "unused",
                name = "Space name",
                lastModifiedTs = "Last modified",
                thumbnail = null,
                resultCount = 5,
                isDefaultSpace = false,
                isShared = false,
                isPublic = false,
                userACL = SpaceACLLevel.Owner
            ),
            showDescriptions = showDescriptions,
            showRemoveSpaceConfirmationDialog = dialogState,
            toggleShowDescriptions = {}
        )
    }
}

@LandscapePreviews
@PortraitPreviews
@Composable
fun SpaceDetailToolbarPreviewNoTitle() {
    OneBooleanPreviewContainer { showDescriptions ->
        val dialogState = remember { mutableStateOf(false) }
        SpaceDetailToolbar(
            rememberLazyListState(),
            space = Space(
                id = "unused",
                name = "Space name",
                lastModifiedTs = "Last modified",
                thumbnail = null,
                resultCount = 5,
                isDefaultSpace = false,
                isShared = false,
                isPublic = false,
                ownerName = "Name of the owner",
                userACL = SpaceACLLevel.Owner
            ),
            showDescriptions = showDescriptions,
            showRemoveSpaceConfirmationDialog = dialogState,
            toggleShowDescriptions = {}
        )
    }
}
