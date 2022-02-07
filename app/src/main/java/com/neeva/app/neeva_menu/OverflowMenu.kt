package com.neeva.app.neeva_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.getClickableAlpha

@Composable
fun OverflowMenu(onMenuItem: (NeevaMenuItemId) -> Unit) {
    val browserWrapper = LocalBrowserWrapper.current
    val activeTabModelState by browserWrapper.activeTabModel.navigationInfoFlow.collectAsState()

    val disabledMenuItems = mutableListOf(
        NeevaMenuItemId.SHARE,
        NeevaMenuItemId.DOWNLOADS,
        NeevaMenuItemId.FEEDBACK
    )
    if (!activeTabModelState.canGoForward) {
        disabledMenuItems.add(NeevaMenuItemId.FORWARD)
    }

    OverflowMenu(onMenuItem = onMenuItem, disabledMenuItems = disabledMenuItems)
}

@Composable
fun OverflowMenu(
    onMenuItem: (NeevaMenuItemId) -> Unit,
    disabledMenuItems: List<NeevaMenuItemId>,
    isInitiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(id = R.string.toolbar_neeva_menu),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NeevaMenuData.iconMenuRowItems.forEach { data ->
                    val isEnabled = !disabledMenuItems.contains(data.id)

                    Column(
                        modifier = Modifier
                            .clickable(enabled = isEnabled) {
                                expanded = false
                                onMenuItem(data.id)
                            }
                            .padding(horizontal = Dimensions.PADDING_SMALL)
                            .widthIn(min = 48.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .alpha(getClickableAlpha(isEnabled)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NeevaMenuIcon(itemData = data)
                        Text(
                            modifier = Modifier.padding(top = Dimensions.PADDING_TINY),
                            text = stringResource(id = data.labelId),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            NeevaMenuData.menuItems.forEach { data ->
                val isEnabled = !disabledMenuItems.contains(data.id)
                val alpha = if (isEnabled) 1.0f else 0.25f

                DropdownMenuItem(
                    enabled = isEnabled,
                    modifier = Modifier.alpha(alpha),
                    onClick = {
                        expanded = false
                        onMenuItem(data.id)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeevaMenuIcon(itemData = data)
                        Text(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            text = stringResource(id = data.labelId),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

class OverflowMenuPreviews : BooleanPreviewParameterProvider<OverflowMenuPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val expanded: Boolean,
        val isForwardEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        expanded = booleanArray[1],
        isForwardEnabled = booleanArray[2]
    )

    @Preview(name = "1x font size", locale = "en")
    @Preview(name = "2x font size", locale = "en", fontScale = 2.0f)
    @Preview(name = "RTL, 1x font size", locale = "he")
    @Preview(name = "RTL, 2x font size", locale = "he", fontScale = 2.0f)
    @Composable
    fun OverflowMenu_Preview(@PreviewParameter(OverflowMenuPreviews::class) params: Params) {
        val disabledMenuItems = if (params.isForwardEnabled) {
            emptyList()
        } else {
            mutableListOf(NeevaMenuItemId.FORWARD)
        }

        NeevaTheme(useDarkTheme = params.darkTheme) {
            OverflowMenu(
                onMenuItem = {},
                isInitiallyExpanded = params.expanded,
                disabledMenuItems = disabledMenuItems
            )
        }
    }
}
