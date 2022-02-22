package com.neeva.app.neeva_menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.neeva.app.R

enum class NeevaMenuItemId {
    HOME,
    SPACES,
    SETTINGS,
    FEEDBACK,
    HISTORY,
    DOWNLOADS,
    FORWARD,
    REFRESH,
    SHARE,
    SHOW_PAGE_INFO,
    FIND_IN_PAGE,
    UPDATE
}

data class NeevaMenuItemData(
    val id: NeevaMenuItemId,
    @StringRes val labelId: Int,
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
            id = NeevaMenuItemId.REFRESH,
            labelId = R.string.refresh,
            icon = Icons.Default.Refresh
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SHARE,
            labelId = R.string.share,
            icon = Icons.Default.Share
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SHOW_PAGE_INFO,
            labelId = R.string.page_info,
            icon = Icons.Default.Info
        )
    )

    val tiles: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            id = NeevaMenuItemId.HOME,
            labelId = R.string.home,
            icon = Icons.Default.Home
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SPACES,
            labelId = R.string.spaces,
            imageResourceID = R.drawable.ic_baseline_bookmarks_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SETTINGS,
            labelId = R.string.settings_main_title,
            icon = Icons.Default.Settings
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.FEEDBACK,
            labelId = R.string.feedback,
            imageResourceID = R.drawable.ic_baseline_feedback_24
        )
    )

    val rows: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            id = NeevaMenuItemId.HISTORY,
            labelId = R.string.history,
            imageResourceID = R.drawable.ic_baseline_history_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.DOWNLOADS,
            labelId = R.string.downloads,
            imageResourceID = R.drawable.ic_baseline_download_24
        ),
    )

    private val dropDownOnly: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            id = NeevaMenuItemId.UPDATE,
            labelId = R.string.update_available,
            icon = Icons.Default.Warning
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.FIND_IN_PAGE,
            labelId = R.string.find_in_page,
            icon = Icons.Default.Search
        ),
    )

    val menuItems: List<NeevaMenuItemData> = dropDownOnly + tiles + rows
}
