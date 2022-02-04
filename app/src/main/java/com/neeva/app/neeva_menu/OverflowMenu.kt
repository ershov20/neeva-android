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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun OverflowMenu(
    onMenuItem: (NeevaMenuItemId) -> Unit,
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
                    Column(
                        modifier = Modifier
                            .clickable {
                                expanded = false
                                onMenuItem(data.id)
                            }
                            .padding(horizontal = 8.dp)
                            .widthIn(min = 48.dp)
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NeevaMenuIcon(itemData = data)
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(id = data.labelId),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            NeevaMenuData.menuItems.forEach { data ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    onMenuItem(data.id)
                }) {
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

class OverflowMenuPreviews : BooleanPreviewParameterProvider<OverflowMenuPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val expanded: Boolean,
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        expanded = booleanArray[1],
    )

    @Preview(name = "1x font size", locale = "en")
    @Preview(name = "2x font size", locale = "en", fontScale = 2.0f)
    @Preview(name = "RTL, 1x font size", locale = "he")
    @Preview(name = "RTL, 2x font size", locale = "he", fontScale = 2.0f)
    @Composable
    fun OverflowMenu_Preview(@PreviewParameter(OverflowMenuPreviews::class) params: Params) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            OverflowMenu(onMenuItem = {}, isInitiallyExpanded = params.expanded)
        }
    }
}
