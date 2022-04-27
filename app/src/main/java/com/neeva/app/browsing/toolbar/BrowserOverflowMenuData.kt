package com.neeva.app.browsing.toolbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.neeva.app.R
import com.neeva.app.overflowmenu.OverflowMenuData
import com.neeva.app.overflowmenu.OverflowMenuIconRowItem
import com.neeva.app.overflowmenu.OverflowMenuItem
import com.neeva.app.overflowmenu.OverflowMenuItemId
import com.neeva.app.ui.widgets.RowActionIconParams

/** Creates the overflow menu that is used when browsing the web. */
fun createBrowserOverflowMenuData(
    isIconRowVisible: Boolean,
    isForwardEnabled: Boolean,
    isUpdateAvailableVisible: Boolean,
    isDesktopUserAgentEnabled: Boolean,
    enableShowDesktopSite: Boolean = false
): OverflowMenuData {
    val additionalIconItems = if (isIconRowVisible) {
        listOf(
            OverflowMenuIconRowItem(
                id = OverflowMenuItemId.FORWARD,
                labelId = R.string.toolbar_go_forward,
                action = RowActionIconParams.ActionType.FORWARD,
                enabled = isForwardEnabled
            ),
            OverflowMenuIconRowItem(
                id = OverflowMenuItemId.RELOAD,
                labelId = R.string.reload,
                action = RowActionIconParams.ActionType.REFRESH
            ),
            OverflowMenuIconRowItem(
                id = OverflowMenuItemId.SHOW_PAGE_INFO,
                labelId = R.string.page_info,
                action = RowActionIconParams.ActionType.SHOW_PAGE_INFO
            )
        )
    } else {
        emptyList()
    }

    val rowItems = mutableListOf<OverflowMenuItem>().apply {
        if (isUpdateAvailableVisible) {
            add(
                OverflowMenuItem(
                    id = OverflowMenuItemId.UPDATE,
                    labelId = R.string.update_available,
                    icon = Icons.Default.Warning
                )
            )
        }

        add(
            OverflowMenuItem(
                id = OverflowMenuItemId.FIND_IN_PAGE,
                labelId = R.string.find_in_page,
                imageResourceID = R.drawable.ic_find_in_page_black_24
            )
        )

        if (enableShowDesktopSite) {
            add(
                if (isDesktopUserAgentEnabled) {
                    OverflowMenuItem(
                        id = OverflowMenuItemId.TOGGLE_DESKTOP_SITE,
                        labelId = R.string.mobile_site,
                        imageResourceID = R.drawable.ic_mobile
                    )
                } else {
                    OverflowMenuItem(
                        id = OverflowMenuItemId.TOGGLE_DESKTOP_SITE,
                        labelId = R.string.desktop_site,
                        imageResourceID = R.drawable.ic_desktop
                    )
                }
            )
        }

        add(OverflowMenuItem(id = OverflowMenuItemId.SEPARATOR))
    }

    return OverflowMenuData(
        isBadgeVisible = isUpdateAvailableVisible,
        iconItems = additionalIconItems,
        additionalRowItems = rowItems
    )
}
