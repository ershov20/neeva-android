package com.neeva.app.history

import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.daos.SitePlusVisit
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.suggestions.NavSuggestionRow
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.ClickableRow
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.RowActionStartIconParams
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.flow.flowOf

@Composable
fun HistoryUI(
    onClose: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    faviconCache: FaviconCache
) {
    val domainProvider = LocalEnvironment.current.domainProvider
    val historyManager = LocalEnvironment.current.historyManager
    val snackbarModel = LocalEnvironment.current.popupModel
    val context = LocalContext.current

    val allHistory = historyManager.getHistoryAfter(Date(0L)).collectAsLazyPagingItems()

    HistoryUI(
        allHistory = allHistory,
        onClearHistory = onClearHistory,
        onClose = onClose,
        onOpenUrl = onOpenUrl,
        onDeleteVisit = { visitUID, siteLabel ->
            historyManager.markVisitForDeletion(visitUID, isMarkedForDeletion = true)
            snackbarModel.showSnackbar(
                message = context.getString(R.string.history_removed_visit, siteLabel),
                actionLabel = context.getString(R.string.undo),
                onActionPerformed = {
                    historyManager.markVisitForDeletion(visitUID, isMarkedForDeletion = false)
                }
            )
        },
        faviconCache = faviconCache,
        domainProvider = domainProvider
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryUI(
    allHistory: LazyPagingItems<SitePlusVisit>,
    onClose: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    onDeleteVisit: (visitUID: Int, siteLabel: String) -> Unit,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider
) {
    Scaffold(
        topBar = @Composable {
            FullScreenDialogTopBar(
                title = stringResource(R.string.history),
                onBackPressed = onClose
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                ClickableRow(
                    primaryLabel = stringResource(R.string.settings_clear_browsing_data),
                    actionIconParams = RowActionIconParams(
                        onTapAction = onClearHistory,
                        actionType = RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN,
                        size = Dimensions.SIZE_ICON_SMALL
                    )
                )
            }

            itemsIndexed(
                items = allHistory,
                key = { _, site -> site.visit.visitUID }
            ) { index, site ->
                val previousTimestamp = (index - 1)
                    .takeIf { it >= 0 }
                    ?.let { allHistory[index - 1]?.visit?.timestamp?.toLocalDate() }
                val currentTimestamp = site?.visit?.timestamp?.toLocalDate()

                val showDate = when {
                    currentTimestamp == null -> false
                    previousTimestamp == null -> true
                    else -> currentTimestamp != previousTimestamp
                }

                site?.let {
                    val timestamp = it.visit.timestamp

                    if (showDate) {
                        val formatted = SimpleDateFormat.getDateInstance().format(timestamp)

                        BaseRowLayout {
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HistoryEntry(site, faviconCache, domainProvider, onOpenUrl, onDeleteVisit)
                }
            }
        }
    }
}

private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun HistoryEntry(
    sitePlusVisit: SitePlusVisit,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider,
    onOpenUrl: (Uri) -> Unit,
    onDeleteVisit: (visitUID: Int, siteLabel: String) -> Unit
) {
    val site = sitePlusVisit.site
    val navSuggestion = site.toNavSuggestion(domainProvider)
    val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(navSuggestion.url)

    NavSuggestionRow(
        iconParams = RowActionStartIconParams(faviconBitmap = faviconBitmap),
        primaryLabel = navSuggestion.label,
        secondaryLabel = navSuggestion.secondaryLabel,
        onTapRow = { onOpenUrl(navSuggestion.url) },
        actionIconParams = RowActionIconParams(
            onTapAction = {
                onDeleteVisit(sitePlusVisit.visit.visitUID, navSuggestion.label)
            },
            actionType = RowActionIconParams.ActionType.DELETE
        )
    )
}

@Preview
@Composable
fun HistoryUI_Preview_Light() = HistoryUI_Preview(useDarkTheme = false)

@Preview
@Composable
fun HistoryUI_Preview_Dark() = HistoryUI_Preview(useDarkTheme = true)

@Composable
private fun HistoryUI_Preview(useDarkTheme: Boolean) {
    var ids = 0

    fun createSitePlusVisit(timestamp: Date): SitePlusVisit {
        val site = Site(
            siteUID = ids++,
            siteURL = "https://www.site$ids.com/",
            title = null,
            largestFavicon = null
        )

        // The only useful value here is the ID.
        val visit = Visit(
            visitUID = ids++,
            timestamp = timestamp,
            visitedSiteUID = site.siteUID
        )

        return SitePlusVisit(site, visit)
    }

    // Add items across several days.
    val allHistory = mutableListOf<SitePlusVisit>().apply {
        for (i in 0 until 7) {
            val currentDate = Calendar.getInstance().apply {
                set(2022, 4, 7 - i)
            }

            add(createSitePlusVisit(currentDate.time))
            add(createSitePlusVisit(currentDate.time))
        }
    }

    val allHistoryFlow = flowOf(PagingData.from(allHistory))

    NeevaThemePreviewContainer(useDarkTheme = useDarkTheme) {
        HistoryUI(
            allHistory = allHistoryFlow.collectAsLazyPagingItems(),
            onClearHistory = {},
            onClose = {},
            onOpenUrl = {},
            onDeleteVisit = { _, _ -> },
            faviconCache = mockFaviconCache,
            domainProvider = mockFaviconCache.domainProvider
        )
    }
}
