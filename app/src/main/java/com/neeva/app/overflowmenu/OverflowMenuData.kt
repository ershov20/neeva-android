package com.neeva.app.overflowmenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.neeva.app.R
import com.neeva.app.ui.widgets.menu.MenuAction
import com.neeva.app.ui.widgets.menu.MenuIconItemData
import com.neeva.app.ui.widgets.menu.MenuRowItem

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
    @StringRes labelId: Int,
    @DrawableRes imageResourceID: Int? = null,
    icon: ImageVector? = null
) = MenuAction(
    id = id.ordinal,
    labelId = labelId,
    imageResourceID = imageResourceID,
    icon = icon
)

class OverflowMenuData(
    val isBadgeVisible: Boolean = false,
    val iconItems: List<MenuIconItemData> = emptyList(),
    additionalRowItems: List<MenuRowItem> = emptyList(),
    showDefaultItems: Boolean = true
) {
    val rowItems: List<MenuRowItem> = additionalRowItems.plus(
        if (showDefaultItems) {
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
                    labelId = R.string.menu_downloads,
                    imageResourceID = R.drawable.ic_download_done_black_24
                )
            )
        } else {
            emptyList()
        }
    )
}
