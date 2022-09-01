// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
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
    val size: Dp = Dimensions.SIZE_ICON_MEDIUM,
    val enabled: Boolean = true
) {
    enum class ActionType {
        NONE,
        ADD,
        BACK,
        COPY,
        DELETE,
        EDIT,
        FORWARD,
        NAVIGATE_TO_SCREEN,
        OPEN_URL,
        REFINE,
        REFRESH,
        SHARE,
        SHOW_PAGE_INFO
    }

    fun getContentDescriptionString(context: Context): String? {
        if (contentDescription != null) return contentDescription

        return when (actionType) {
            ActionType.REFINE -> context.getString(R.string.refine)
            ActionType.REFRESH -> context.getString(R.string.reload)

            else -> null
        }
    }
}

@Composable
fun RowActionIconButton(iconParams: RowActionIconParams) {
    if (iconParams.actionType == RowActionIconParams.ActionType.NONE) return

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        IconButton(
            onClick = iconParams.onTapAction,
            enabled = iconParams.enabled
        ) {
            RowActionIcon(
                actionType = iconParams.actionType,
                contentDescription = iconParams.getContentDescriptionString(LocalContext.current),
                size = iconParams.size
            )
        }
    }
}

@Composable
fun RowActionIcon(
    actionType: RowActionIconParams.ActionType,
    contentDescription: String? = null,
    size: Dp = Dimensions.SIZE_ICON_MEDIUM
) {
    // We need to manually flip directional icons around in case the user is using an RTL layout.
    val modifier = Modifier
        .size(size)
        .then(
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                Modifier.scale(scaleX = -1f, scaleY = 1f)
            } else {
                Modifier
            }
        )
    when (actionType) {
        RowActionIconParams.ActionType.REFINE -> {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_north_west_24),
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.DELETE -> {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.OPEN_URL -> {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_open_in_new_24),
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN -> {
            Icon(
                painter = painterResource(R.drawable.ic_navigate_next),
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.BACK -> {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.FORWARD -> {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.REFRESH -> {
            Icon(
                Icons.Default.Refresh,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.SHOW_PAGE_INFO -> {
            Icon(
                painterResource(id = R.drawable.ic_info_black_24),
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.SHARE -> {
            Icon(
                Icons.Default.Share,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.EDIT -> {
            Icon(
                Icons.Default.Edit,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.ADD -> {
            Icon(
                Icons.Default.Add,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        RowActionIconParams.ActionType.COPY -> {
            Icon(
                painterResource(id = R.drawable.ic_content_copy_24),
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        else -> {}
    }
}
