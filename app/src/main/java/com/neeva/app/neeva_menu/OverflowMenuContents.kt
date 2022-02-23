package com.neeva.app.neeva_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.getClickableAlpha

@Composable
fun ColumnScope.OverflowMenuContents(
    onMenuItem: (NeevaMenuItemId) -> Unit,
    disabledMenuItems: List<NeevaMenuItemId>,
    expandedMutator: (Boolean) -> Unit
) {
    val menuItemState = LocalMenuData.current

    Row(
        modifier = Modifier.padding(12.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NeevaMenuData.iconMenuRowItems.forEach { data ->
            val isEnabled = !disabledMenuItems.contains(data.id)

            Column(
                modifier = Modifier
                    .clickable(enabled = isEnabled) {
                        expandedMutator(false)
                        onMenuItem(data.id)
                    }
                    .padding(horizontal = Dimensions.PADDING_SMALL)
                    .widthIn(min = 48.dp)
                    .alpha(getClickableAlpha(isEnabled)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeevaMenuIcon(itemData = data)
                Text(
                    modifier = Modifier.padding(top = Dimensions.PADDING_TINY),
                    text = stringResource(id = data.labelId),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    NeevaMenuData.menuItems.forEach { data ->
        if (data.id == NeevaMenuItemId.UPDATE && !menuItemState.isUpdateAvailableVisible) {
            return@forEach
        }

        val isEnabled = !disabledMenuItems.contains(data.id)
        val alpha = if (isEnabled) 1.0f else 0.25f

        DropdownMenuItem(
            enabled = isEnabled,
            modifier = Modifier.alpha(alpha),
            onClick = {
                expandedMutator(false)
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

class OverflowMenuContentsPreviews :
    BooleanPreviewParameterProvider<OverflowMenuContentsPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val isForwardEnabled: Boolean,
        val isUpdateAvailableVisible: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isForwardEnabled = booleanArray[1],
        isUpdateAvailableVisible = booleanArray[2]
    )

    @Preview(name = "1x font size", locale = "en")
    @Preview(name = "2x font size", locale = "en", fontScale = 2.0f)
    @Preview(name = "RTL, 1x font size", locale = "he")
    @Preview(name = "RTL, 2x font size", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(@PreviewParameter(OverflowMenuContentsPreviews::class) params: Params) {
        val disabledMenuItems = if (params.isForwardEnabled) {
            emptyList()
        } else {
            mutableListOf(NeevaMenuItemId.FORWARD)
        }

        NeevaTheme(useDarkTheme = params.darkTheme) {
            CompositionLocalProvider(
                LocalMenuData provides LocalMenuDataState(
                    isUpdateAvailableVisible = params.isUpdateAvailableVisible
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    OverflowMenuContents(
                        onMenuItem = {},
                        disabledMenuItems = disabledMenuItems,
                        expandedMutator = {}
                    )
                }
            }
        }
    }
}
