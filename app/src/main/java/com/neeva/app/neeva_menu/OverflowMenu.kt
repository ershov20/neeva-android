package com.neeva.app.neeva_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer

// TODO(dan.alcantara): Don't like how I set this up; will redo this once Evan's PR lands and move
//                      disabled item state into here.
data class LocalMenuDataState(
    val isUpdateAvailableVisible: Boolean
)
val LocalMenuData = compositionLocalOf {
    LocalMenuDataState(isUpdateAvailableVisible = false)
}

@Composable
fun OverflowMenu(
    onMenuItem: (NeevaMenuItemId) -> Unit,
    canGoForward: Boolean
) {
    val disabledMenuItems = mutableListOf<NeevaMenuItemId>()

    if (!canGoForward) {
        disabledMenuItems.add(NeevaMenuItemId.FORWARD)
    }

    OverflowMenu(
        onMenuItem = onMenuItem,
        disabledMenuItems = disabledMenuItems
    )
}

@Composable
fun OverflowMenu(
    onMenuItem: (NeevaMenuItemId) -> Unit,
    disabledMenuItems: List<NeevaMenuItemId>,
    isInitiallyExpanded: Boolean = false
) {
    val menuItemState = LocalMenuData.current
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.toolbar_neeva_menu)
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
            modifier = Modifier.defaultMinSize(minWidth = 250.dp)
        ) {
            OverflowMenuContents(
                onMenuItem = onMenuItem,
                disabledMenuItems = disabledMenuItems,
                expandedMutator = { newState: Boolean -> expanded = newState }
            )
        }
    }
}

@Preview(name = "LTR", locale = "en")
@Preview(name = "RTL", locale = "he")
@Composable
private fun OverflowMenuPreview() {
    OneBooleanPreviewContainer { isUpdateAvailableVisible ->
        Surface {
            CompositionLocalProvider(
                LocalMenuData provides LocalMenuDataState(
                    isUpdateAvailableVisible = isUpdateAvailableVisible
                )
            ) {
                OverflowMenu(
                    onMenuItem = {},
                    disabledMenuItems = emptyList(),
                    isInitiallyExpanded = false
                )
            }
        }
    }
}
