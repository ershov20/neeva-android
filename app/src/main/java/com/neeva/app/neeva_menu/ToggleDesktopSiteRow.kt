package com.neeva.app.neeva_menu

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.neeva.app.R

@Composable
fun ToggleDesktopSiteRow(desktopUserAgentEnabled: Boolean, onClick: () -> Unit) {
    val data = if (desktopUserAgentEnabled) {
        NeevaMenuItemData(
            id = NeevaMenuItemId.TOGGLE_DESKTOP_SITE,
            labelId = R.string.mobile_site,
            imageResourceID = R.drawable.ic_mobile
        )
    } else {
        NeevaMenuItemData(
            id = NeevaMenuItemId.TOGGLE_DESKTOP_SITE,
            labelId = R.string.desktop_site,
            imageResourceID = R.drawable.ic_desktop
        )
    }

    DropdownMenuItem(
        leadingIcon = {
            NeevaMenuIcon(itemData = data)
        },
        text = {
            OverflowMenuText(data.labelId?.let { stringResource(id = it) } ?: "")
        },
        modifier = Modifier.alpha(1f),
        onClick = onClick
    )
}
