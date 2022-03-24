package com.neeva.app.spaces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.storage.entities.Space
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.widgets.overlay.OverlaySheet
import com.neeva.app.ui.widgets.overlay.OverlaySheetConfig

@Composable
fun AddToSpaceSheet(
    webLayerModel: WebLayerModel,
    spaceModifier: SpaceModifier
) {
    val appNavModel = LocalAppNavModel.current
    val spaceStore = LocalEnvironment.current.spaceStore

    val browserWrapper by webLayerModel.currentBrowserFlow.collectAsState()
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
    val spaces: List<Space> by spaceStore.editableSpacesFlow.collectAsState(emptyList())

    LazyColumn {
        stickyHeader {
            BaseRowLayout(
                endComposable = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                }
            ) {
                Text(
                    "Save to Spaces",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
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
