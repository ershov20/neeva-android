package com.neeva.app.neeva_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.getClickableAlpha
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@Composable
fun OverflowMenuContents(
    hideButtons: Boolean,
    onMenuItem: (NeevaMenuItemId) -> Unit,
    disabledMenuItems: List<NeevaMenuItemId>,
    desktopUserAgentEnabled: Boolean,
    enableShowDesktopSite: Boolean = true,
    expandedMutator: (Boolean) -> Unit,
) {
    val menuItemState = LocalMenuData.current
    if (!hideButtons) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NeevaMenuData.iconMenuRowItems.forEach { data ->
                val isEnabled = !disabledMenuItems.contains(data.id)
                RowActionIconButton(
                    RowActionIconParams(
                        onTapAction = {
                            expandedMutator(false)
                            onMenuItem(data.id)
                        },
                        actionType = data.action,
                        contentDescription = stringResource(id = data.labelId),
                        enabled = isEnabled
                    )
                )
            }
        }
    }

    NeevaMenuData.menuItems.forEach { data ->
        if (data.id == NeevaMenuItemId.UPDATE && !menuItemState.isUpdateAvailableVisible) {
            return@forEach
        }

        when (data.id) {
            NeevaMenuItemId.SEPARATOR -> {
                Box(modifier = Modifier.padding(Dimensions.PADDING_TINY)) {
                    Spacer(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .height(1.dp)
                            .fillMaxWidth()
                    )
                }
            }

            NeevaMenuItemId.TOGGLE_DESKTOP_SITE -> {
                if (enableShowDesktopSite) {
                    ToggleDesktopSiteRow(desktopUserAgentEnabled) {
                        expandedMutator(false)
                        onMenuItem(NeevaMenuItemId.TOGGLE_DESKTOP_SITE)
                    }
                }
            }

            else -> {
                val isEnabled = !disabledMenuItems.contains(data.id)
                val alpha = getClickableAlpha(isEnabled)

                DropdownMenuItem(
                    leadingIcon = {
                        NeevaMenuIcon(itemData = data)
                    },
                    text = {
                        OverflowMenuText(data.labelId?.let { stringResource(id = it) } ?: "")
                    },
                    enabled = isEnabled,
                    modifier = Modifier.alpha(alpha),
                    onClick = {
                        expandedMutator(false)
                        onMenuItem(data.id)
                    }
                )
            }
        }
    }
}

class OverflowMenuContentsPreviews :
    BooleanPreviewParameterProvider<OverflowMenuContentsPreviews.Params>(5) {
    data class Params(
        val darkTheme: Boolean,
        val isForwardEnabled: Boolean,
        val isUpdateAvailableVisible: Boolean,
        val hideButtons: Boolean,
        val desktopUserAgentEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isForwardEnabled = booleanArray[1],
        isUpdateAvailableVisible = booleanArray[2],
        hideButtons = booleanArray[3],
        desktopUserAgentEnabled = booleanArray[4]
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
            Surface(tonalElevation = 2.dp) {
                CompositionLocalProvider(
                    LocalMenuData provides LocalMenuDataState(
                        isUpdateAvailableVisible = params.isUpdateAvailableVisible
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        OverflowMenuContents(
                            onMenuItem = {},
                            disabledMenuItems = disabledMenuItems,
                            desktopUserAgentEnabled = params.desktopUserAgentEnabled,
                            expandedMutator = {},
                            hideButtons = params.hideButtons
                        )
                    }
                }
            }
        }
    }
}
