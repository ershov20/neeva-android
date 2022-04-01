package com.neeva.app.spaces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.storage.entities.Space

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddToSpaceUI(
    activeTabModel: ActiveTabModel,
    spaceStore: SpaceStore,
    spaceModifier: SpaceModifier
) {
    val spaces: List<Space> by spaceStore.editableSpacesFlow.collectAsState(emptyList())

    LazyColumn {
        items(spaces) {
            SpaceRow(space = it, activeTabModel) {
                spaceModifier.addOrRemoveCurrentTabToSpace(it)
            }
        }
    }
}
