package com.neeva.app.overflowmenu

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.browsing.toolbar.createBrowserOverflowMenuData
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.getClickableAlpha
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@Composable
fun OverflowMenuContents(
    overflowMenuData: OverflowMenuData,
    onMenuItem: (OverflowMenuItemId) -> Unit,
    expandedMutator: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        overflowMenuData.iconItems.forEach { data ->
            RowActionIconButton(
                RowActionIconParams(
                    onTapAction = {
                        expandedMutator(false)
                        onMenuItem(data.id)
                    },
                    actionType = data.action,
                    contentDescription = stringResource(id = data.labelId),
                    enabled = data.enabled
                )
            )
        }
    }

    overflowMenuData.rowItems.forEach { data ->
        when (data.id) {
            OverflowMenuItemId.SEPARATOR -> {
                Box(modifier = Modifier.padding(Dimensions.PADDING_TINY)) {
                    Spacer(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .height(1.dp)
                            .fillMaxWidth()
                    )
                }
            }

            else -> {
                val isEnabled = data.enabled
                val alpha = getClickableAlpha(isEnabled)

                DropdownMenuItem(
                    leadingIcon = {
                        OverflowMenuIcon(itemData = data)
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

class OverflowMenuContentsPreviews {
    @Composable
    internal fun DefaultPreview(
        darkTheme: Boolean,
        isUpdateAvailableVisible: Boolean,
        desktopUserAgentEnabled: Boolean,
        isForwardEnabled: Boolean = true
    ) {
        NeevaTheme(useDarkTheme = darkTheme) {
            Surface(tonalElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OverflowMenuContents(
                        overflowMenuData = createBrowserOverflowMenuData(
                            isForwardEnabled = isForwardEnabled,
                            isUpdateAvailableVisible = isUpdateAvailableVisible,
                            isDesktopUserAgentEnabled = desktopUserAgentEnabled,
                            enableShowDesktopSite = true
                        ),
                        onMenuItem = {},
                        expandedMutator = {}
                    )
                }
            }
        }
    }

    @Preview(name = "DefaultPreview LTR, 1x font size", locale = "en")
    @Preview(name = "DefaultPreview LTR, 2x font size", locale = "en", fontScale = 2.0f)
    @Preview(name = "DefaultPreview RTL", locale = "he")
    @Composable
    fun PreviewLight_ForwardEnabled_UpdateAvailableVisible_DesktopSite() {
        DefaultPreview(
            darkTheme = false,
            isForwardEnabled = true,
            isUpdateAvailableVisible = true,
            desktopUserAgentEnabled = true
        )
    }

    @Preview(name = "PreviewDark_ForwardEnabled_UpdateAvailableVisible_DesktopSite", locale = "en")
    @Composable
    fun PreviewDark_ForwardEnabled_UpdateAvailableVisible_DesktopSite() {
        DefaultPreview(
            darkTheme = true,
            isForwardEnabled = true,
            isUpdateAvailableVisible = true,
            desktopUserAgentEnabled = true
        )
    }

    @Preview(name = "PreviewLight_ForwardDisabled_DesktopSite", locale = "en")
    @Composable
    fun PreviewLight_ForwardDisabled_DesktopSite() {
        DefaultPreview(
            darkTheme = false,
            isForwardEnabled = false,
            isUpdateAvailableVisible = false,
            desktopUserAgentEnabled = true
        )
    }

    @Preview(name = "PreviewLight_ForwardEnabled_MobileSite", locale = "en")
    @Composable
    fun PreviewLight_ForwardEnabled_MobileSite() {
        DefaultPreview(
            darkTheme = false,
            isForwardEnabled = true,
            isUpdateAvailableVisible = false,
            desktopUserAgentEnabled = false
        )
    }
}
