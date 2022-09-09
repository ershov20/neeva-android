// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid.archived

import android.graphics.Bitmap
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalPopupModel
import com.neeva.app.R
import com.neeva.app.storage.entities.TabData
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.widgets.RowActionStartIcon
import com.neeva.app.ui.widgets.RowActionStartIconParams
import com.neeva.app.ui.widgets.StackedText
import com.neeva.app.ui.widgets.menu.MenuAction
import com.neeva.app.ui.widgets.menu.MenuContent

/**
 * Displays information about an archived tab.  If the tab being rendered is a PagedList placeholder
 * (signified by [tabData] being null), we still draw the container to ensure that it takes up the
 * same amount of space it would when it gets loaded.
 */
@Composable
fun ArchivedTab(
    faviconCache: FaviconCache,
    tabData: TabData?,
    onRestoreArchivedTab: (tabData: TabData) -> Unit,
    onDeleteArchivedTab: (tabData: TabData) -> Unit,
) {
    val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(tabData?.url)
    val popupModel = LocalPopupModel.current

    val onTapRow = if (tabData != null) {
        { onRestoreArchivedTab(tabData) }
    } else {
        null
    }

    val onLongTap = if (tabData != null) {
        {
            popupModel.showContextMenu { onDismissRequested ->
                MenuContent(menuItems = listOf(MenuAction(id = R.string.delete))) { id ->
                    when (id) {
                        R.string.delete -> onDeleteArchivedTab(tabData)
                    }

                    onDismissRequested()
                }
            }
        }
    } else {
        null
    }

    BaseRowLayout(
        startComposable = {
            RowActionStartIcon(
                RowActionStartIconParams(faviconBitmap = faviconBitmap)
            )
        },
        onTapRow = onTapRow,
        onLongTap = onLongTap,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
    ) {
        StackedText(
            primaryLabel = tabData?.title ?: "",
            primaryMaxLines = 2,
            primaryTextStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
