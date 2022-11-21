// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid.archived

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.neeva.app.LocalDispatchers
import com.neeva.app.LocalHistoryManager
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.browsing.ArchiveAfterOption
import com.neeva.app.cardgrid.CardGrid
import com.neeva.app.history.HistoryHeader
import com.neeva.app.sharedprefs.SharedPrefFolder.App.AutomaticallyArchiveTabs
import com.neeva.app.storage.entities.TabData
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.previewFaviconCache
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.widgets.ClickableRow
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

@Composable
fun ArchivedTabGrid(
    faviconCache: FaviconCache,
    onRestoreArchivedTab: (tabData: TabData) -> Unit,
    onDeleteArchivedTab: (tabData: TabData) -> Unit,
    onDeleteAllArchivedTabs: () -> Unit
) {
    var isArchivingDialogVisibleState by remember { mutableStateOf(false) }
    val pagedArchivedTabs = LocalHistoryManager.current.getPagedArchivedTabs()
        .collectAsLazyPagingItems()
    val archiveAfterOption by AutomaticallyArchiveTabs
        .getFlow(LocalSharedPreferencesModel.current)
        .collectAsState()

    ArchivedTabGrid(
        tabs = pagedArchivedTabs,
        archiveAfterOption = archiveAfterOption,
        faviconCache = faviconCache,
        onShowArchiveAfterOptionDialog = { isArchivingDialogVisibleState = true },
        onRestoreArchivedTab = onRestoreArchivedTab,
        onDeleteArchivedTab = onDeleteArchivedTab,
        onDeleteAllArchivedTabs = onDeleteAllArchivedTabs
    )

    if (isArchivingDialogVisibleState) {
        ArchivingOptionsDialog(
            onDismissDialog = { isArchivingDialogVisibleState = false }
        )
    }
}

@Composable
fun ArchivedTabGrid(
    tabs: LazyPagingItems<TabData>,
    archiveAfterOption: ArchiveAfterOption,
    faviconCache: FaviconCache,
    onShowArchiveAfterOptionDialog: () -> Unit,
    onRestoreArchivedTab: (tabData: TabData) -> Unit,
    onDeleteArchivedTab: (tabData: TabData) -> Unit,
    onDeleteAllArchivedTabs: () -> Unit
) {
    // Each item triggered by the [items] call inside of a [LazyVerticalGrid] expects that only one
    // Composable will be drawn per item: we can't add both a date header that spans the whole row
    // at the same time that we add an archived tab underneath it.  This means that we have to
    // pre-process the paged items to figure out where to insert date rows and pass that processed
    // list to the [items] call.
    val dispatchers = LocalDispatchers.current
    var processedTabs by remember { mutableStateOf<ProcessedArchivedTabs?>(null) }
    LaunchedEffect(tabs.itemSnapshotList) {
        snapshotFlow { ProcessedArchivedTabs(tabs) }
            .flowOn(dispatchers.io)
            .distinctUntilChanged()
            .collectLatest { processedTabs = it }
    }

    CardGrid(modifier = Modifier.fillMaxWidth()) { numCells ->
        item(span = { GridItemSpan(numCells) }) {
            ClickableRow(
                primaryLabel = stringResource(R.string.archived_tabs_archive),
                secondaryLabel = stringResource(archiveAfterOption.resourceId),
                primaryMaxLines = Int.MAX_VALUE,
                secondaryMaxLines = Int.MAX_VALUE,
                onTapAction = onShowArchiveAfterOptionDialog,
            )
        }

        item(span = { GridItemSpan(numCells) }) {
            ClickableRow(
                primaryLabel = stringResource(R.string.archived_tabs_clear_archived),
                primaryMaxLines = Int.MAX_VALUE,
                isDangerousAction = true,
                onTapAction = onDeleteAllArchivedTabs,
            )
        }

        processedTabs?.let { processedTabs ->
            // As the user scrolls up or down, Compose chooses which items from [processedTabs] to
            // load and display.  The indices don't necessarily start at 0 when rendering and the
            // indices chosen aren't always loaded sequentially.
            items(
                count = processedTabs.entries.size,
                key = { index -> processedTabs.key(index) },
                span = { index -> processedTabs.span(index, numCells) }
            ) { index ->
                val entry = processedTabs.entries[index]
                if (entry.date != null) {
                    HistoryHeader(entry.date)
                } else {
                    ArchivedTab(
                        faviconCache = faviconCache,
                        tabData = processedTabs.getTabData(index),
                        onRestoreArchivedTab = onRestoreArchivedTab,
                        onDeleteArchivedTab = onDeleteArchivedTab
                    )
                }
            }
        }
    }
}

@PortraitPreviews
@Composable
fun PreviewArchivedTabGrid() {
    PreviewArchivedTabGrid(useDarkTheme = false)
}

@PortraitPreviewsDark
@Composable
fun PreviewArchivedTabGrid_Dark() {
    PreviewArchivedTabGrid(useDarkTheme = true)
}

@Composable
fun PreviewArchivedTabGrid(useDarkTheme: Boolean) {
    val now = System.currentTimeMillis()

    NeevaThemePreviewContainer(
        useDarkTheme = useDarkTheme,
        addBorder = false
    ) {
        val tabs = (0 until 100).map {
            TabData(
                id = "tab $it",
                url = Uri.parse("https://www.neeva.com/$it"),
                title = "Title $it",
                lastActiveMs = now - TimeUnit.DAYS.toMillis(2L * it),
                isArchived = true
            )
        }
        val pagingTabs = PagingData.from(tabs.toList())

        ArchivedTabGrid(
            tabs = flowOf(pagingTabs).collectAsLazyPagingItems(),
            archiveAfterOption = ArchiveAfterOption.AFTER_7_DAYS,
            faviconCache = previewFaviconCache,
            onShowArchiveAfterOptionDialog = {},
            onRestoreArchivedTab = {},
            onDeleteArchivedTab = {},
            onDeleteAllArchivedTabs = {}
        )
    }
}
