package com.neeva.app.spaces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpaceDetail() {
    val showDescriptionsPreferenceKey = "SHOW_DESCRIPTIONS"
    val spaceStore = LocalEnvironment.current.spaceStore
    val spaces = spaceStore.allSpacesFlow.collectAsState()
    val spaceID = spaceStore.detailedSpaceIDFlow.collectAsState()
    val space = remember(spaceID) {
        spaces.value.find { it.id == spaceID.value }
    }
    val content = getSpaceContentsAsync(spaceID = spaceID.value)
    val sharedPrefs = LocalEnvironment.current.sharedPreferencesModel
    val showDescriptions = remember {
        mutableStateOf(
            sharedPrefs.getValue(
                SharedPrefFolder.SPACES,
                showDescriptionsPreferenceKey,
                defaultValue = false
            )
        )
    }

    LazyColumn {
        stickyHeader {
            SpaceDetailToolbar(
                space = space,
                showDescriptions = showDescriptions.value
            ) {
                sharedPrefs.setValue(
                    SharedPrefFolder.SPACES,
                    showDescriptionsPreferenceKey,
                    !showDescriptions.value
                )
                showDescriptions.value = !showDescriptions.value
            }
        }

        if (space != null) {
            item {
                SpaceHeader(space = space)
            }
        }

        content.value?.let { content ->
            items(content, key = { it.id }) { spaceItem ->
                SpaceItemDetail(spaceItem = spaceItem, showDescriptions = showDescriptions.value)
            }
        }
    }
}

@Composable
fun SpaceDetailToolbar(
    space: Space?,
    showDescriptions: Boolean,
    toggleShowDescriptions: () -> Unit
) {
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
        title = {},
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

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.defaultMinSize(minWidth = 250.dp)
            ) {
                // Add Unfollow here.
            }
        }

    )
}

/** Returns a [State] that can be used in a Composable for contents of a Space. */
@Composable
fun getSpaceContentsAsync(spaceID: String?): State<List<SpaceItem>?> {
    val spaceStore = LocalEnvironment.current.spaceStore
    return produceState<List<SpaceItem>?>(initialValue = null, spaceID) {
        val id = spaceID ?: return@produceState
        value = spaceStore.contentDataForSpace(spaceID = id)
    }
}

@Preview
@Composable
fun SpaceDetailToolbarPreview() {
    OneBooleanPreviewContainer { showDescriptions ->
        SpaceDetailToolbar(
            space = null,
            showDescriptions = showDescriptions,
            toggleShowDescriptions = {}
        )
    }
}
