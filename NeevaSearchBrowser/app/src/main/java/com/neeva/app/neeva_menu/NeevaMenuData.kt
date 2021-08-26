package com.neeva.app.neeva_menu

import android.net.Uri
import com.neeva.app.AppNavState
import com.neeva.app.appSpacesURL
import com.neeva.app.appURL
import com.neeva.app.R

data class NeevaMenuItemData(
    val label: String,
    val contentDescription: String,
    val imageResourceID: Int,
    val onClick: () -> Unit
)

object NeevaMenuData {
    lateinit var updateState: (AppNavState) -> Unit
    lateinit var loadURL: (Uri) -> Unit
    val data: List<NeevaMenuItemData> = listOf(
        NeevaMenuItemData(
            label = "Home",
            contentDescription = "Home Button",
            imageResourceID = R.drawable.ic_baseline_home_24,
            onClick = {
                loadURL(Uri.parse(appURL))
                updateState(AppNavState.HIDDEN)
            }
        ),
        NeevaMenuItemData(
            label = "Spaces",
            contentDescription = "Spaces Button",
            imageResourceID = R.drawable.ic_baseline_bookmarks_24,
            onClick = {
                loadURL(Uri.parse(appSpacesURL))
                updateState(AppNavState.HIDDEN)
            }
        ),
        NeevaMenuItemData(
            label = "Settings",
            contentDescription = "Settings Button",
            imageResourceID = R.drawable.ic_baseline_settings_24,
            onClick = {
                updateState(AppNavState.SETTINGS)
            }
        ),
        NeevaMenuItemData(
            label = "Feedback",
            contentDescription = "Feedback Button",
            imageResourceID = R.drawable.ic_baseline_feedback_24,
            onClick = {}
        ),
        NeevaMenuItemData(
            label = "History",
            contentDescription = "History Button",
            imageResourceID = R.drawable.ic_baseline_history_24,
            onClick = {
                updateState(AppNavState.HISTORY)
            }
        ),
        NeevaMenuItemData(
            label = "Downloads",
            contentDescription = "Downloads Button",
            imageResourceID = R.drawable.ic_baseline_download_24,
            onClick = {}
        ),
    )
}