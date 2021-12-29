package com.neeva.app.neeva_menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neeva.app.R

enum class NeevaMenuItemId {
    HOME,
    SPACES,
    SETTINGS,
    FEEDBACK,
    HISTORY,
    DOWNLOADS
}

data class NeevaMenuItemData(
    val id: NeevaMenuItemId,
    @StringRes val labelId: Int,
    @DrawableRes val imageResourceID: Int
)

object NeevaMenuData {
    val tiles: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            id = NeevaMenuItemId.HOME,
            labelId = R.string.home,
            imageResourceID = R.drawable.ic_baseline_home_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SPACES,
            labelId = R.string.spaces,
            imageResourceID = R.drawable.ic_baseline_bookmarks_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SETTINGS,
            labelId = R.string.settings,
            imageResourceID = R.drawable.ic_baseline_settings_24
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
}