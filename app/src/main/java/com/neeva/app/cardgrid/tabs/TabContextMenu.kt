// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid.tabs

import androidx.compose.runtime.Composable
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.ui.widgets.menu.MenuAction
import com.neeva.app.ui.widgets.menu.MenuContent

@Composable
fun TabContextMenu(
    tab: TabInfo,
    browserWrapper: BrowserWrapper,
    onDismissRequested: () -> Unit
) {
    val menuItems = listOf(
        if (tab.data.isPinned) {
            MenuAction(id = R.string.menu_unpin_tab)
        } else {
            MenuAction(id = R.string.menu_pin_tab)
        }
    )

    MenuContent(menuItems = menuItems) { id ->
        when (id) {
            R.string.menu_pin_tab -> {
                browserWrapper.pinTab(tab.id, true)
            }

            R.string.menu_unpin_tab -> {
                browserWrapper.pinTab(tab.id, false)
            }
        }

        onDismissRequested()
    }
}
