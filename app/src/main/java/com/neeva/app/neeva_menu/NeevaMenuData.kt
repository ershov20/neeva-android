package com.neeva.app.neeva_menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.neeva.app.R

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
    SEPARATOR
}

data class NeevaMenuItemData(
    val id: NeevaMenuItemId,
    @StringRes val labelId: Int? = null,
    @DrawableRes val imageResourceID: Int? = null,
    val icon: ImageVector? = null
)

object NeevaMenuData {
    val iconMenuRowItems: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            id = NeevaMenuItemId.FORWARD,
            labelId = R.string.toolbar_go_forward,
            icon = Icons.Default.ArrowForward
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.RELOAD,
            labelId = R.string.reload,
            icon = Icons.Default.Refresh
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SHOW_PAGE_INFO,
            labelId = R.string.page_info,
            imageResourceID = R.drawable.ic_info_black_24
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
