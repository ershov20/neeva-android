package com.neeva.app.cardgrid.spaces

import android.net.Uri
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.Dispatchers
import com.neeva.app.LocalDispatchers
import com.neeva.app.LocalNeevaUser
import com.neeva.app.LocalSpaceStore
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.cardgrid.CardGrid
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.previewDispatchers
import com.neeva.app.spaces.SpacesIntro
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.previewCardGridTitles
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SpaceGrid(
    browserWrapper: BrowserWrapper,
    cardsPaneModel: CardsPaneModel,
    dispatchers: Dispatchers = LocalDispatchers.current,
    neevaConstants: NeevaConstants,
    modifier: Modifier = Modifier
) {
    val spaceStore = LocalSpaceStore.current
    val neevaUser = LocalNeevaUser.current
    val spaces by spaceStore.allSpacesFlow.collectAsState(emptyList())

    // TODO(dan.alcantara): Find a better place to trigger these refreshes.
    LaunchedEffect(true) {
        spaceStore.refresh()
    }

    if (neevaUser.isSignedOut()) {
        SpacesIntro(includeSpaceCard = true)
    } else {
        SpaceGrid(
            onSelectSpace = { spaceUrl -> cardsPaneModel.selectSpace(browserWrapper, spaceUrl) },
            spaces = spaces,
            itemProvider = { spaceId -> spaceStore.contentDataForSpace(spaceId) },
            dispatchers = dispatchers,
            neevaConstants = neevaConstants,
            modifier = modifier
        )
    }
}

@Composable
fun SpaceGrid(
    onSelectSpace: (spaceUrl: Uri) -> Unit,
    spaces: List<Space>,
    itemProvider: suspend (spaceId: String) -> List<SpaceItem>,
    dispatchers: Dispatchers,
    neevaConstants: NeevaConstants,
    modifier: Modifier = Modifier
) {
    CardGrid(
        items = spaces,
        modifier = modifier,
        emptyComposable = { /* Empty state shows nothing on iOS */ },
    ) { _, listItems ->
        items(listItems, key = { it.id }) { space ->
            SpaceCard(
                spaceId = space.id,
                spaceName = space.name,
                isSpacePublic = space.isPublic,
                onSelect = { onSelectSpace(space.url(neevaConstants)) },
                itemProvider = itemProvider,
                dispatchers = dispatchers
            )
        }
    }
}

class SpacesGridPreviews : BooleanPreviewParameterProvider<SpacesGridPreviews.Params>(1) {
    data class Params(
        val darkTheme: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0]
    )

    @Preview("1x", locale = "en")
    @Preview("2x", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x", locale = "he")
    @Preview("Landscape, 1x scale", widthDp = 1024, heightDp = 600, locale = "en")
    @Composable
    fun DefaultPreview(@PreviewParameter(SpacesGridPreviews::class) params: Params) {
        val darkTheme = params.darkTheme

        NeevaTheme(useDarkTheme = darkTheme) {
            val spaces = mutableListOf<Space>()

            val spaceNames = previewCardGridTitles.take(5)
            spaceNames.forEachIndexed { index, spaceName ->
                spaces.add(
                    Space(
                        id = "space$index",
                        name = spaceName,
                        lastModifiedTs = "",
                        thumbnail = null,
                        resultCount = 5,
                        isDefaultSpace = false,
                        isShared = false,
                        isPublic = true,
                        userACL = SpaceACLLevel.Owner
                    )
                )
            }

            SpaceGrid(
                onSelectSpace = {},
                spaces = spaces,
                itemProvider = {
                    mutableListOf<SpaceItem>().apply {
                        for (i in 0 until 6) {
                            SpaceItem(
                                id = "",
                                spaceID = "",
                                url = null,
                                title = "",
                                snippet = null,
                                thumbnail = null
                            )
                        }
                    }
                },
                dispatchers = previewDispatchers,
                neevaConstants = NeevaConstants()
            )
        }
    }
}
