package com.neeva.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.ui.OneBooleanPreviewContainer
import kotlinx.coroutines.flow.StateFlow

data class TabToolbarModel(
    val onAddToSpace: () -> Unit = {},
    val onTabSwitcher: () -> Unit = {},
    val goBack: () -> Unit = {},
    val share: () -> Unit = {}
)

/**  Contains all the controls available to the user in the bottom toolbar. */
@Composable
fun BottomToolbar(
    bottomControlOffset: StateFlow<Float>,
    modifier: Modifier = Modifier
) {
    val appNavModel = LocalAppNavModel.current
    val browserWrapper = LocalBrowserWrapper.current

    val bottomOffset by bottomControlOffset.collectAsState()
    val bottomOffsetDp = with(LocalDensity.current) { bottomOffset.toDp() }
    BottomToolbar(
        model = TabToolbarModel(
            onAddToSpace = appNavModel::showAddToSpace,
            onTabSwitcher = {
                browserWrapper.takeScreenshotOfActiveTab {
                    appNavModel.showCardGrid()
                }
            },
            goBack = browserWrapper::goBack,
            share = appNavModel::shareCurrentPage
        ),
        activeTabModel = browserWrapper.activeTabModel,
        isIncognito = browserWrapper.isIncognito,
        modifier = modifier.offset(y = bottomOffsetDp)
    )
}

@Composable
fun BottomToolbar(
    model: TabToolbarModel,
    activeTabModel: ActiveTabModel,
    isIncognito: Boolean,
    modifier: Modifier
) {
    val navigationInfo by activeTabModel.navigationInfoFlow.collectAsState()
    val spaceStoreHasUrl by activeTabModel.isCurrentUrlInSpaceFlow.collectAsState()

    BottomToolbar(
        model = model,
        canGoBackward = navigationInfo.canGoBackward,
        isIncognito = isIncognito,
        spaceStoreHasUrl = spaceStoreHasUrl,
        modifier = modifier
    )
}

@Composable
fun BottomToolbar(
    model: TabToolbarModel,
    canGoBackward: Boolean,
    isIncognito: Boolean,
    spaceStoreHasUrl: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = backgroundColor,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.bottom_toolbar_height))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            IconButton(
                enabled = canGoBackward,
                onClick = model.goBack,
                modifier = Modifier.weight(1.0f)
            ) {
                // Material3 chooses "onSurface" as the disabled color and doesn't seem to allow you
                // to change it.  Because we use a similar color for Incognito backgrounds,
                // manually set the color to what we wanted with the alpha they pick to avoid making
                // the button invisible.
                val backTint = contentColorFor(backgroundColor = backgroundColor).copy(
                    alpha = LocalContentColor.current.alpha
                )
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.toolbar_go_back),
                    tint = backTint
                )
            }

            IconButton(
                enabled = true,
                onClick = model.share,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(id = R.string.share)
                )
            }

            IconButton(
                enabled = true,
                onClick = model.onAddToSpace,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    painter = painterResource(
                        if (spaceStoreHasUrl) {
                            R.drawable.ic_baseline_bookmark_24
                        } else {
                            R.drawable.ic_baseline_bookmark_border_24
                        }
                    ),
                    contentDescription = stringResource(R.string.toolbar_save_to_space)
                )
            }

            IconButton(
                enabled = true,
                onClick = model.onTabSwitcher,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_filter_none_24),
                    contentDescription = stringResource(R.string.toolbar_tab_switcher)
                )
            }
        }
    }
}

@Preview("Default, LTR", locale = "en")
@Preview("Default, RTL", locale = "he")
@Composable
private fun BottomToolbarPreview_Regular() {
    OneBooleanPreviewContainer { isIncognito ->
        BottomToolbar(
            model = TabToolbarModel(),
            canGoBackward = false,
            spaceStoreHasUrl = false,
            isIncognito = isIncognito
        )
    }
}

@Preview("Can go backward, LTR", locale = "en")
@Preview("Can go backward, RTL", locale = "he")
@Composable
private fun BottomToolbarPreview_CanGoBackward() {
    OneBooleanPreviewContainer { isIncognito ->
        BottomToolbar(
            model = TabToolbarModel(),
            canGoBackward = true,
            spaceStoreHasUrl = false,
            isIncognito = isIncognito
        )
    }
}

@Preview("Space store has URL, LTR", locale = "en")
@Preview("Space store has URL, RTL", locale = "he")
@Composable
private fun BottomToolbarPreview_SpaceStoreHasUrl() {
    OneBooleanPreviewContainer { isIncognito ->
        BottomToolbar(
            model = TabToolbarModel(),
            canGoBackward = false,
            spaceStoreHasUrl = true,
            isIncognito = isIncognito
        )
    }
}
