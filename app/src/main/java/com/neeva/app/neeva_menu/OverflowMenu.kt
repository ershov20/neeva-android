package com.neeva.app.neeva_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

// TODO(dan.alcantara): Don't like how I set this up; will redo this once Evan's PR lands and move
//                      disabled item state into here.
data class LocalMenuDataState(
    val isUpdateAvailableVisible: Boolean
)
val LocalMenuData = compositionLocalOf<LocalMenuDataState> { error("No value set") }

@Composable
fun OverflowMenu(
    onMenuItem: (NeevaMenuItemId) -> Unit,
    foregroundColor: Color
) {
    val browserWrapper = LocalBrowserWrapper.current
    val activeTabModelState by browserWrapper.activeTabModel.navigationInfoFlow.collectAsState()

    val disabledMenuItems = mutableListOf(
        NeevaMenuItemId.DOWNLOADS
    )

    if (!activeTabModelState.canGoForward) {
        disabledMenuItems.add(NeevaMenuItemId.FORWARD)
    }

    OverflowMenu(
        onMenuItem = onMenuItem,
        disabledMenuItems = disabledMenuItems,
        foregroundColor = foregroundColor
    )
}

@Composable
fun OverflowMenu(
    onMenuItem: (NeevaMenuItemId) -> Unit,
    disabledMenuItems: List<NeevaMenuItemId>,
    foregroundColor: Color,
    isInitiallyExpanded: Boolean = false
) {
    val menuItemState = LocalMenuData.current
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.toolbar_neeva_menu),
                    tint = foregroundColor
                )

                if (menuItemState.isUpdateAvailableVisible) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }

        // The dropdown's width is arbitrarily set to 250.dp to avoid Compose shrinking the menu to
        // wrap the menu item contents.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .defaultMinSize(minWidth = 250.dp)
        ) {
            OverflowMenuContents(
                onMenuItem = onMenuItem,
                disabledMenuItems = disabledMenuItems,
                expandedMutator = { newState: Boolean -> expanded = newState }
            )
        }
    }
}

class OverflowMenuPreviews : BooleanPreviewParameterProvider<OverflowMenuPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val isUpdateAvailableVisible: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isUpdateAvailableVisible = booleanArray[1]
    )

    @Preview(name = "1x font size", locale = "en")
    @Preview(name = "2x font size", locale = "en", fontScale = 2.0f)
    @Preview(name = "RTL, 1x font size", locale = "he")
    @Preview(name = "RTL, 2x font size", locale = "he", fontScale = 2.0f)
    @Composable
    fun OverflowMenu_Preview(@PreviewParameter(OverflowMenuPreviews::class) params: Params) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            CompositionLocalProvider(
                LocalMenuData provides LocalMenuDataState(
                    isUpdateAvailableVisible = params.isUpdateAvailableVisible
                )
            ) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    OverflowMenu(
                        onMenuItem = {},
                        disabledMenuItems = emptyList(),
                        foregroundColor = MaterialTheme.colorScheme.onSurface,
                        isInitiallyExpanded = false
                    )
                }
            }
        }
    }
}
