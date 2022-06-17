package com.neeva.app.overflowmenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.neeva.app.R
import com.neeva.app.ui.widgets.menu.MenuIconItemData
import com.neeva.app.ui.widgets.menu.MenuItemType
import com.neeva.app.ui.widgets.menu.MenuRowData

enum class OverflowMenuItemId {
    SETTINGS,
    SUPPORT,
    HISTORY,
    DOWNLOADS,
    FORWARD,
    RELOAD,
    SHOW_PAGE_INFO,
    FIND_IN_PAGE,
    UPDATE,
    TOGGLE_DESKTOP_SITE,
    SPACES_WEBSITE,
    CLOSE_ALL_TABS,
    SEPARATOR
}

fun overflowMenuItem(
    id: OverflowMenuItemId,
    @StringRes labelId: Int? = null,
    @DrawableRes imageResourceID: Int? = null,
    icon: ImageVector? = null
) = MenuRowData(
    type = MenuItemType.ACTION,
    id = id.ordinal,
    labelId = labelId,
    imageResourceID = imageResourceID,
    icon = icon
)

class OverflowMenuData(
    val isBadgeVisible: Boolean = false,
    val iconItems: List<MenuIconItemData> = emptyList(),
    additionalRowItems: List<MenuRowData> = emptyList()
) {
    val rowItems: List<MenuRowData> = additionalRowItems.plus(
        listOf(
            overflowMenuItem(
                id = OverflowMenuItemId.SUPPORT,
                labelId = R.string.feedback,
                imageResourceID = R.drawable.ic_help_outline_black_24
            ),
            overflowMenuItem(
                id = OverflowMenuItemId.SETTINGS,
                labelId = R.string.settings,
                imageResourceID = R.drawable.ic_settings_black_24
            ),
            overflowMenuItem(
                id = OverflowMenuItemId.HISTORY,
                labelId = R.string.history,
                imageResourceID = R.drawable.ic_baseline_history_24
            ),
            overflowMenuItem(
                id = OverflowMenuItemId.DOWNLOADS,
                labelId = R.string.downloads,
                imageResourceID = R.drawable.ic_download_done_black_24
            )
        )
    )
}
