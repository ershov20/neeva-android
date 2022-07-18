package com.neeva.app.browsing.toolbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.neeva.app.R
import com.neeva.app.overflowmenu.OverflowMenuData
import com.neeva.app.overflowmenu.OverflowMenuItemId
import com.neeva.app.overflowmenu.overflowMenuItem
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.menu.MenuIconItemData
import com.neeva.app.ui.widgets.menu.MenuRowItem
import com.neeva.app.ui.widgets.menu.MenuSeparator

/** Creates the overflow menu that is used when browsing the web. */
fun createBrowserOverflowMenuData(
    isForwardEnabled: Boolean,
    isUpdateAvailableVisible: Boolean,
    isDesktopUserAgentEnabled: Boolean,
    enableShowDesktopSite: Boolean = false
): OverflowMenuData {
    val iconItems = listOf(
        MenuIconItemData(
            id = OverflowMenuItemId.FORWARD.ordinal,
            labelId = R.string.toolbar_go_forward,
            action = RowActionIconParams.ActionType.FORWARD,
            enabled = isForwardEnabled
        ),
        MenuIconItemData(
            id = OverflowMenuItemId.RELOAD.ordinal,
            labelId = R.string.reload,
            action = RowActionIconParams.ActionType.REFRESH
        ),
        MenuIconItemData(
            id = OverflowMenuItemId.SHOW_PAGE_INFO.ordinal,
            labelId = R.string.page_info,
            action = RowActionIconParams.ActionType.SHOW_PAGE_INFO
        )
    )

    val rowItems = mutableListOf<MenuRowItem>().apply {
        if (isUpdateAvailableVisible) {
            add(
                overflowMenuItem(
                    id = OverflowMenuItemId.UPDATE,
                    labelId = R.string.menu_update_available,
                    icon = Icons.Default.Warning
                )
            )
        }

        add(
            overflowMenuItem(
                id = OverflowMenuItemId.FIND_IN_PAGE,
                labelId = R.string.find_in_page,
                imageResourceID = R.drawable.ic_find_in_page_black_24
            )
        )

        if (enableShowDesktopSite) {
            add(
                if (isDesktopUserAgentEnabled) {
                    overflowMenuItem(
                        id = OverflowMenuItemId.TOGGLE_DESKTOP_SITE,
                        labelId = R.string.menu_mobile_site,
                        imageResourceID = R.drawable.ic_mobile
                    )
                } else {
                    overflowMenuItem(
                        id = OverflowMenuItemId.TOGGLE_DESKTOP_SITE,
                        labelId = R.string.menu_desktop_site,
                        imageResourceID = R.drawable.ic_desktop
                    )
                }
            )
        }

        add(MenuSeparator)
    }

    return OverflowMenuData(
        isBadgeVisible = isUpdateAvailableVisible,
        iconItems = iconItems,
        additionalRowItems = rowItems
    )
}
