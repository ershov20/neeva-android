package com.neeva.app.neeva_menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.neeva.app.R
import com.neeva.app.ui.widgets.RowActionIconParams

enum class NeevaMenuItemId {
    HOME,
    SETTINGS,
    SUPPORT,
    HISTORY,
    DOWNLOADS,
    FORWARD,
    RELOAD,
    SHARE,
    SHOW_PAGE_INFO,
    FIND_IN_PAGE,
    UPDATE,
    TOGGLE_DESKTOP_SITE,
    SEPARATOR
}

data class NeevaMenuTopBarItemData(
    val id: NeevaMenuItemId,
    @StringRes val labelId: Int,
    val action: RowActionIconParams.ActionType
)

data class NeevaMenuItemData(
    val id: NeevaMenuItemId,
    @StringRes val labelId: Int? = null,
    @DrawableRes val imageResourceID: Int? = null,
    val icon: ImageVector? = null
)

object NeevaMenuData {
    val iconMenuRowItems: List<NeevaMenuTopBarItemData> = listOf(
        NeevaMenuTopBarItemData(
            id = NeevaMenuItemId.FORWARD,
            labelId = R.string.toolbar_go_forward,
            action = RowActionIconParams.ActionType.FORWARD
        ),
        NeevaMenuTopBarItemData(
            id = NeevaMenuItemId.RELOAD,
            labelId = R.string.reload,
            action = RowActionIconParams.ActionType.REFRESH
        ),
        NeevaMenuTopBarItemData(
            id = NeevaMenuItemId.SHOW_PAGE_INFO,
            labelId = R.string.page_info,
            action = RowActionIconParams.ActionType.SHOW_PAGE_INFO
        )
    )

    val menuItems: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            id = NeevaMenuItemId.UPDATE,
            labelId = R.string.update_available,
            icon = Icons.Default.Warning
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.FIND_IN_PAGE,
            labelId = R.string.find_in_page,
            imageResourceID = R.drawable.ic_find_in_page_black_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.TOGGLE_DESKTOP_SITE
        ),
        NeevaMenuItemData(id = NeevaMenuItemId.SEPARATOR),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SUPPORT,
            labelId = R.string.feedback,
            imageResourceID = R.drawable.ic_help_outline_black_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SETTINGS,
            labelId = R.string.settings,
            imageResourceID = R.drawable.ic_settings_black_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.HISTORY,
            labelId = R.string.history,
            imageResourceID = R.drawable.ic_baseline_history_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.DOWNLOADS,
            labelId = R.string.downloads,
            imageResourceID = R.drawable.ic_download_done_black_24
        )
    )
}
