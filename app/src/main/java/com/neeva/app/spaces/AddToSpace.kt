package com.neeva.app.spaces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.storage.entities.Space
import com.neeva.app.ui.layouts.BaseRowLayout

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddToSpaceUI(
    activeTabModel: ActiveTabModel,
    spaceStore: SpaceStore,
    spaceModifier: SpaceModifier
) {
    val spaces: List<Space> by spaceStore.editableSpacesFlow.collectAsState(emptyList())
    val neevaUser = LocalEnvironment.current.neevaUser

    if (neevaUser.isSignedOut()) {
        SpacesIntro(includeSpaceCard = false)
    } else {
        LazyColumn {
            item {
                val spaceStore = LocalEnvironment.current.spaceStore

                BaseRowLayout(
                    onTapRow = spaceStore::createSpace,
                    startComposable = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.create_space),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(spaces, key = { it.id }) {
                SpaceRow(space = it, activeTabModel) {
                    spaceModifier.addOrRemoveCurrentTabToSpace(it)
                }
            }
        }
    }
}
