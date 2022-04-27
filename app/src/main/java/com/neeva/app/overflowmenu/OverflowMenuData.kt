package com.neeva.app.overflowmenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.neeva.app.R
import com.neeva.app.ui.widgets.RowActionIconParams

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

data class OverflowMenuIconRowItem(
    val id: OverflowMenuItemId,
    @StringRes val labelId: Int,
    val action: RowActionIconParams.ActionType,
    val enabled: Boolean = true
)

data class OverflowMenuItem(
    val id: OverflowMenuItemId,
    @StringRes val labelId: Int? = null,
    @DrawableRes val imageResourceID: Int? = null,
    val icon: ImageVector? = null,
    val enabled: Boolean = true
)

class OverflowMenuData(
    val isBadgeVisible: Boolean = false,
    val iconItems: List<OverflowMenuIconRowItem> = emptyList(),
    additionalRowItems: List<OverflowMenuItem> = emptyList()
) {
    val rowItems: List<OverflowMenuItem> = additionalRowItems.plus(
        mutableListOf(
            OverflowMenuItem(
                id = OverflowMenuItemId.SUPPORT,
                labelId = R.string.feedback,
                imageResourceID = R.drawable.ic_help_outline_black_24
            ),
            OverflowMenuItem(
                id = OverflowMenuItemId.SETTINGS,
                labelId = R.string.settings,
                imageResourceID = R.drawable.ic_settings_black_24
            ),
            OverflowMenuItem(
                id = OverflowMenuItemId.HISTORY,
                labelId = R.string.history,
                imageResourceID = R.drawable.ic_baseline_history_24
            ),
            OverflowMenuItem(
                id = OverflowMenuItemId.DOWNLOADS,
                labelId = R.string.downloads,
                imageResourceID = R.drawable.ic_download_done_black_24
            )
        )
    )
}
