package com.neeva.app.cardgrid.tabs

import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.browsing.ArchiveAfterOption
import com.neeva.app.browsing.TabInfo
import com.neeva.app.history.HistoryHeader
import com.neeva.app.sharedprefs.SharedPrefFolder.App.AutomaticallyArchiveTabs
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
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun ArchivedTabsList(
    tabs: List<TabInfo>,
    faviconCache: FaviconCache,
    onTabSelected: (id: String) -> Unit,
    onClearArchivedTabs: () -> Unit
) {
    val archiveAfterOption = AutomaticallyArchiveTabs
        .getFlow(LocalSharedPreferencesModel.current)
        .collectAsState()
    ArchivedTabsList(
        tabs = tabs,
        archiveAfterOption = archiveAfterOption.value,
        faviconCache = faviconCache,
        onTabSelected = onTabSelected,
        onClearArchivedTabs = onClearArchivedTabs
    )
}

@Composable
fun ArchivedTabsList(
    tabs: List<TabInfo>,
    archiveAfterOption: ArchiveAfterOption,
    faviconCache: FaviconCache,
    onTabSelected: (id: String) -> Unit,
    onClearArchivedTabs: () -> Unit
) {
    val isArchivingDialogVisibleState = remember { mutableStateOf(false) }

    // It'd be more correct to make the time be a state, too, but it'd be expensive because
    // we would be re-filtering the list every time the time changed.
    val now = System.currentTimeMillis()

    val visibleTabs by remember(tabs, archiveAfterOption) {
        derivedStateOf {
            tabs
                .filter { it.isArchived(archiveAfterOption, now) }
                .sortedByDescending { it.data.lastActiveMs }
        }
    }

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
                    onTapAction = onClearArchivedTabs,
                    actionType = RowActionIconParams.ActionType.NONE
                )
            )
        }

        itemsIndexed(
            items = visibleTabs,
            key = { _, tabInfo -> tabInfo.id }
        ) { index, tabInfo ->
            val previousTimestamp = (index - 1)
                .takeIf { it >= 0 }
                ?.let { visibleTabs[index - 1].data.lastActiveMs.toLocalDate() }
            val currentTimestamp = tabInfo.data.lastActiveMs.toLocalDate()

            val showDate = when (previousTimestamp) {
                null -> true
                else -> currentTimestamp != previousTimestamp
            }

            if (showDate) {
                HistoryHeader(
                    SimpleDateFormat.getDateInstance().format(Date(tabInfo.data.lastActiveMs))
                )
            }

            val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(tabInfo.url)
            NavSuggestionRow(
                iconParams = RowActionStartIconParams(faviconBitmap = faviconBitmap),
                primaryLabel = tabInfo.title ?: "",
                secondaryLabel = tabInfo.url?.toString(),
                onTapRow = { onTabSelected(tabInfo.id) }
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
                TabInfo(
                    id = "tab $it",
                    url = Uri.parse("https://www.neeva.com/$it"),
                    title = "Title $it",
                    isSelected = false,
                    data = TabInfo.PersistedData(
                        lastActiveMs = now - TimeUnit.DAYS.toMillis(2L * it)
                    )
                )
            }
            .toList()

        ArchivedTabsList(
            tabs = tabs,
            archiveAfterOption = ArchiveAfterOption.AFTER_7_DAYS,
            faviconCache = previewFaviconCache,
            onTabSelected = {},
            onClearArchivedTabs = {}
        )
    }
}
