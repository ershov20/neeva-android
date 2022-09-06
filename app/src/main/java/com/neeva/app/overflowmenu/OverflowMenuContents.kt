// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.overflowmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.browsing.toolbar.createBrowserOverflowMenuData
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.menu.MenuContent

@Composable
fun OverflowMenuContents(
    overflowMenuData: OverflowMenuData,
    onMenuItem: (Int) -> Unit
) {
    MenuContent(
        menuRows = overflowMenuData.rowItems,
        menuIconItems = overflowMenuData.iconItems
    ) { id ->
        onMenuItem(id)
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
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OverflowMenuContents(
                        overflowMenuData = createBrowserOverflowMenuData(
                            isForwardEnabled = isForwardEnabled,
                            isUpdateAvailableVisible = isUpdateAvailableVisible,
                            isDesktopUserAgentEnabled = desktopUserAgentEnabled
                        ),
                        onMenuItem = {}
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
