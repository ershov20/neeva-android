// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid.tabs

import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.neeva.app.LocalHistoryManager
import com.neeva.app.LocalPopupModel
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.browsing.ArchiveAfterOption
import com.neeva.app.history.HistoryHeader
import com.neeva.app.sharedprefs.SharedPrefFolder.App.AutomaticallyArchiveTabs
import com.neeva.app.storage.entities.TabData
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.previewFaviconCache
import com.neeva.app.suggestions.NavSuggestionRow
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.toLocalDate
import com.neeva.app.ui.widgets.ClickableRow
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.RowActionStartIconParams
import com.neeva.app.ui.widgets.menu.MenuAction
import com.neeva.app.ui.widgets.menu.MenuContent
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf

@Composable
fun ArchivedTabsList(
    faviconCache: FaviconCache,
    onRestoreArchivedTab: (tabData: TabData) -> Unit,
    onDeleteArchivedTab: (tabData: TabData) -> Unit,
    onDeleteAllArchivedTabs: () -> Unit
) {
    val historyManager = LocalHistoryManager.current
    val pagedArchivedTabs = historyManager.getPagedArchivedTabs().collectAsLazyPagingItems()

    val archiveAfterOption = AutomaticallyArchiveTabs
        .getFlow(LocalSharedPreferencesModel.current)
        .collectAsState()
    ArchivedTabsList(
        tabs = pagedArchivedTabs,
        archiveAfterOption = archiveAfterOption.value,
        faviconCache = faviconCache,
        onRestoreArchivedTab = onRestoreArchivedTab,
        onDeleteArchivedTab = onDeleteArchivedTab,
        onDeleteAllArchivedTabs = onDeleteAllArchivedTabs
    )
}

@Composable
fun ArchivedTabsList(
    tabs: LazyPagingItems<TabData>,
    archiveAfterOption: ArchiveAfterOption,
    faviconCache: FaviconCache,
    onRestoreArchivedTab: (tabData: TabData) -> Unit,
    onDeleteArchivedTab: (tabData: TabData) -> Unit,
    onDeleteAllArchivedTabs: () -> Unit
) {
    val popupModel = LocalPopupModel.current
    val isArchivingDialogVisibleState = remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            ClickableRow(
                primaryLabel = stringResource(R.string.archived_tabs_archive),
                secondaryLabel = stringResource(archiveAfterOption.resourceId),
                primaryMaxLines = Int.MAX_VALUE,
                secondaryMaxLines = Int.MAX_VALUE,
                actionIconParams = RowActionIconParams(
                    onTapAction = { isArchivingDialogVisibleState.value = true },
                    actionType = RowActionIconParams.ActionType.NONE
                )
            )
        }

        item {
            ClickableRow(
                primaryLabel = stringResource(R.string.archived_tabs_clear_archived),
                primaryMaxLines = Int.MAX_VALUE,
                isActionDangerous = true,
                actionIconParams = RowActionIconParams(
                    onTapAction = onDeleteAllArchivedTabs,
                    actionType = RowActionIconParams.ActionType.NONE
                )
            )
        }

        itemsIndexed(
            items = tabs,
            key = { _, item -> item.id }
        ) { index, tabInfo ->
            tabInfo ?: return@itemsIndexed

            val previousDate = (index - 1)
                .takeIf { it >= 0 }
                ?.let { tabs[index - 1]?.lastActiveMs?.toLocalDate() }
            val currentTimestamp = tabInfo.lastActiveMs
            val currentDate = currentTimestamp.toLocalDate()

            if (previousDate == null || currentDate != previousDate) {
                HistoryHeader(
                    SimpleDateFormat.getDateInstance().format(Date(currentTimestamp))
                )
            }

            val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(tabInfo.url)
            NavSuggestionRow(
                iconParams = RowActionStartIconParams(faviconBitmap = faviconBitmap),
                primaryLabel = tabInfo.title ?: "",
                secondaryLabel = tabInfo.url?.toString(),
                onTapRow = { onRestoreArchivedTab(tabInfo) },
                onLongPress = {
                    popupModel.showContextMenu { onDismissRequested ->
                        MenuContent(
                            menuItems = listOf(MenuAction(id = R.string.delete))
                        ) { id ->
                            when (id) {
                                R.string.delete -> onDeleteArchivedTab(tabInfo)
                            }

                            onDismissRequested()
                        }
                    }
                }
            )
        }
    }

    if (isArchivingDialogVisibleState.value) {
        ArchivingOptionsDialog(
            onDismissDialog = { isArchivingDialogVisibleState.value = false }
        )
    }
}

@PortraitPreviews
@Composable
fun PreviewArchivedTabsList() {
    PreviewArchivedTabsList(useDarkTheme = false)
}

@PortraitPreviewsDark
@Composable
fun PreviewArchivedTabsList_Dark() {
    PreviewArchivedTabsList(useDarkTheme = true)
}

@Composable
fun PreviewArchivedTabsList(useDarkTheme: Boolean) {
    val now = System.currentTimeMillis()

    NeevaThemePreviewContainer(
        useDarkTheme = useDarkTheme,
        addBorder = false
    ) {
        val tabs = (0 until 100)
            .map {
                TabData(
                    id = "tab $it",
                    url = Uri.parse("https://www.neeva.com/$it"),
                    title = "Title $it",
                    lastActiveMs = now - TimeUnit.DAYS.toMillis(2L * it),
                    isArchived = true
                )
            }
            .toList()
        val pagingTabs = PagingData.from(tabs)

        ArchivedTabsList(
            tabs = flowOf(pagingTabs).collectAsLazyPagingItems(),
            archiveAfterOption = ArchiveAfterOption.AFTER_7_DAYS,
            faviconCache = previewFaviconCache,
            onRestoreArchivedTab = {},
            onDeleteArchivedTab = {},
            onDeleteAllArchivedTabs = {}
        )
    }
}
