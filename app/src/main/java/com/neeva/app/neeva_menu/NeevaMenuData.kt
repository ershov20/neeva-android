package com.neeva.app.neeva_menu

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
    val label: String,
    val contentDescription: String,
    val imageResourceID: Int
)

object NeevaMenuData {
    val data: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            id = NeevaMenuItemId.HOME,
            label = "Home",
            contentDescription = "Home Button",
            imageResourceID = R.drawable.ic_baseline_home_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SPACES,
            label = "Spaces",
            contentDescription = "Spaces Button",
            imageResourceID = R.drawable.ic_baseline_bookmarks_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.SETTINGS,
            label = "Settings",
            contentDescription = "Settings Button",
            imageResourceID = R.drawable.ic_baseline_settings_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.FEEDBACK,
            label = "Feedback",
            contentDescription = "Feedback Button",
            imageResourceID = R.drawable.ic_baseline_feedback_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.HISTORY,
            label = "History",
            contentDescription = "History Button",
            imageResourceID = R.drawable.ic_baseline_history_24
        ),
        NeevaMenuItemData(
            id = NeevaMenuItemId.DOWNLOADS,
            label = "Downloads",
            contentDescription = "Downloads Button",
            imageResourceID = R.drawable.ic_baseline_download_24
        ),
    )
}