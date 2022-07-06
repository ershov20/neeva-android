package com.neeva.app.spaces

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalNeevaUser
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.storage.entities.Space
import com.neeva.app.ui.layouts.BaseRowLayout

@Composable
fun AddToSpaceUI(
    activeTabModel: ActiveTabModel,
    spaceStore: SpaceStore,
    dismissSheet: () -> Unit,
    spaceModifier: SpaceModifier
) {
    val spaces: List<Space> by spaceStore.editableSpacesFlow.collectAsState(emptyList())
    val spacesWithURL by activeTabModel.spacesContainingCurrentUrlFlow.collectAsState()
    val displayedSpaces by remember {
        derivedStateOf {
            spaces.sortedByDescending { spacesWithURL.contains(it.id) }
        }
    }

    val neevaUser = LocalNeevaUser.current

    val isCreateSpaceDialogVisible = remember { mutableStateOf(false) }

    if (neevaUser.isSignedOut()) {
        SpacesIntro(
            includeSpaceCard = false,
            dismissSheet = dismissSheet
        )
    } else {
        LazyColumn {
            item {
                BaseRowLayout(
                    onTapRow = { isCreateSpaceDialogVisible.value = true },
                    startComposable = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.space_create),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(displayedSpaces, key = { it.id }) {
                SpaceRow(space = it, spacesWithURL = spacesWithURL) {
                    spaceModifier.addOrRemoveCurrentTabToSpace(it)
                }
            }
        }
    }

    CreateSpaceDialog(
        isDialogVisible = isCreateSpaceDialogVisible,
        onDismissRequested = { isCreateSpaceDialogVisible.value = false }
    )
}
