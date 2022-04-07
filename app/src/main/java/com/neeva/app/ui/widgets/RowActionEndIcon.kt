package com.neeva.app.ui.widgets

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

data class RowActionIconParams(
    val onTapAction: () -> Unit,
    val actionType: ActionType,
    val contentDescription: String? = null,
    val size: Dp = Dimensions.PADDING_MEDIUM
) {
    enum class ActionType {
        REFINE, DELETE, OPEN_URL, NAVIGATE_TO_SCREEN
    }
}

@Composable
fun RowActionIcon(iconParams: RowActionIconParams) {
    // We need to manually flip directional icons around in case the user is using an RTL layout.
    val modifier = Modifier.size(iconParams.size).then(
        if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
            Modifier.scale(scaleX = -1f, scaleY = 1f)
        } else {
            Modifier
        }
    )

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        IconButton(onClick = iconParams.onTapAction) {
            when (iconParams.actionType) {
                RowActionIconParams.ActionType.REFINE -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_north_west_24),
                        contentDescription = iconParams.contentDescription,
                        modifier = modifier
                    )
                }

                RowActionIconParams.ActionType.DELETE -> {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = iconParams.contentDescription,
                        modifier = modifier
                    )
                }

                RowActionIconParams.ActionType.OPEN_URL -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_open_in_new_24),
                        contentDescription = iconParams.contentDescription,
                        modifier = modifier
                    )
                }

                RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_navigate_next),
                        contentDescription = iconParams.contentDescription,
                        modifier = modifier
                    )
                }
            }
        }
    }
}
