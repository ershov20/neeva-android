package com.neeva.app.history

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.daos.SitePlusVisit
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.suggestions.NavSuggestionRow
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.ClickableRow
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.RowActionStartIconParams
import com.neeva.app.ui.widgets.collapsingsection.collapsingSection
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import kotlinx.coroutines.flow.flowOf

@Composable
fun HistoryUI(
    onClose: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    faviconCache: FaviconCache,
    now: LocalDate = LocalDate.now()
) {
    val domainProvider = LocalEnvironment.current.domainProvider
    val historyManager = LocalEnvironment.current.historyManager
    val snackbarModel = LocalEnvironment.current.snackbarModel
    val sharedPrefsModel = LocalEnvironment.current.sharedPreferencesModel
    val context = LocalContext.current

    val historyUIModel = remember { HistoryUIModel(sharedPrefsModel) }

    val startOfTime = Date(0L)
    val startOf7DaysAgo = Date.from(now.minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC))
    val startOfYesterday = Date.from(now.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
    val startOfToday = Date.from(now.atStartOfDay().toInstant(ZoneOffset.UTC))

    // Recreate the Flows only when the 7 day window is updated.  Until then, the history database
    // query will be accurate and Room will continue to update the Flows whenever the user visits
    // a site.
    val historyToday = remember(startOf7DaysAgo) {
        historyManager.getHistoryAfter(startOfToday)
    }.collectAsLazyPagingItems()

    val historyYesterday = remember(startOf7DaysAgo) {
        historyManager.getHistoryBetween(startOfYesterday, startOfToday)
    }.collectAsLazyPagingItems()

    val historyThisWeek = remember(startOf7DaysAgo) {
        historyManager.getHistoryBetween(startOf7DaysAgo, startOfYesterday)
    }.collectAsLazyPagingItems()

    val historyBeforeThisWeek = remember(startOf7DaysAgo) {
        historyManager.getHistoryBetween(startOfTime, startOf7DaysAgo)
    }.collectAsLazyPagingItems()

    HistoryUI(
        historyUIModel = historyUIModel,
        historyToday = historyToday,
        historyYesterday = historyYesterday,
        historyThisWeek = historyThisWeek,
        historyBeforeThisWeek = historyBeforeThisWeek,
        onClearHistory = onClearHistory,
        onClose = onClose,
        onOpenUrl = onOpenUrl,
        onDeleteVisit = { visitUID, siteLabel ->
            historyManager.markVisitForDeletion(visitUID, isMarkedForDeletion = true)
            snackbarModel.show(
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

@Composable
private fun HistoryUI(
    historyUIModel: HistoryUIModel,
    historyToday: LazyPagingItems<SitePlusVisit>,
    historyYesterday: LazyPagingItems<SitePlusVisit>,
    historyThisWeek: LazyPagingItems<SitePlusVisit>,
    historyBeforeThisWeek: LazyPagingItems<SitePlusVisit>,
    onClose: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    onDeleteVisit: (visitUID: Int, siteLabel: String) -> Unit,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider
) {
    val isTodayDisplayed = historyUIModel.getState(HistoryUIPrefs.TodayState)
    val isYesterdayDisplayed = historyUIModel.getState(HistoryUIPrefs.YesterdayState)
    val isThisWeekDisplayed = historyUIModel.getState(HistoryUIPrefs.ThisWeekState)
    val isBeforeThisWeekDisplayed = historyUIModel.getState(HistoryUIPrefs.BeforeThisWeekState)

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        FullScreenDialogTopBar(
            title = stringResource(R.string.history),
            onBackPressed = onClose
        )

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            item {
                ClickableRow(
                    primaryLabel = stringResource(R.string.settings_clear_browsing_data),
                    actionIconParams = RowActionIconParams(
                        onTapAction = onClearHistory,
                        actionType = RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN
                    )
                )
            }

            collapsingSection(
                label = R.string.history_today,
                collapsingSectionState = isTodayDisplayed.value,
                onUpdateCollapsingSectionState = {
                    historyUIModel.advanceState(HistoryUIPrefs.TodayState)
                }
            ) {
                items(historyToday) { site ->
                    site?.let {
                        HistoryEntry(site, faviconCache, domainProvider, onOpenUrl, onDeleteVisit)
                    }
                }
            }

            collapsingSection(
                label = R.string.history_yesterday,
                collapsingSectionState = isYesterdayDisplayed.value,
                onUpdateCollapsingSectionState = {
                    historyUIModel.advanceState(HistoryUIPrefs.YesterdayState)
                }
            ) {
                items(historyYesterday) { site ->
                    site?.let {
                        HistoryEntry(site, faviconCache, domainProvider, onOpenUrl, onDeleteVisit)
                    }
                }
            }

            collapsingSection(
                label = R.string.history_this_week,
                collapsingSectionState = isThisWeekDisplayed.value,
                onUpdateCollapsingSectionState = {
                    historyUIModel.advanceState(HistoryUIPrefs.ThisWeekState)
                }
            ) {
                items(historyThisWeek) { site ->
                    site?.let {
                        HistoryEntry(site, faviconCache, domainProvider, onOpenUrl, onDeleteVisit)
                    }
                }
            }

            collapsingSection(
                label = R.string.history_earlier,
                collapsingSectionState = isBeforeThisWeekDisplayed.value,
                onUpdateCollapsingSectionState = {
                    historyUIModel.advanceState(HistoryUIPrefs.BeforeThisWeekState)
                }
            ) {
                items(historyBeforeThisWeek) { site ->
                    site?.let {
                        HistoryEntry(site, faviconCache, domainProvider, onOpenUrl, onDeleteVisit)
                    }
                }
            }
        }
    }
}

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
fun HistoryUI_Preview() {
    var ids = 0

    fun createSitePlusVisit(): SitePlusVisit {
        val site = Site(
            siteUID = ids++,
            siteURL = "https://www.site$ids.com/",
            title = null,
            largestFavicon = null
        )

        // The only useful value here is the ID.
        val visit = Visit(
            visitUID = ids++,
            timestamp = Date(),
            visitedSiteUID = site.siteUID
        )

        return SitePlusVisit(site, visit)
    }

    // Add items for today.
    val historyToday = mutableListOf<SitePlusVisit>()
    for (i in 0 until 2) {
        historyToday.add(createSitePlusVisit())
    }

    // Add items for yesterday.
    val historyYesterday = mutableListOf<SitePlusVisit>()
    for (i in 0 until 2) {
        historyYesterday.add(createSitePlusVisit())
    }

    // Add one item for each day before that.  Items that are too old should not be displayed.
    val historyThisWeek = mutableListOf<SitePlusVisit>()
    for (daysAgo in 2 until 10) {
        historyThisWeek.add(createSitePlusVisit())
    }

    // Add one item for each day before this week.
    val historyBeforeThisWeek = mutableListOf<SitePlusVisit>()
    for (daysAgo in 10 until 25) {
        historyBeforeThisWeek.add(createSitePlusVisit())
    }

    val itemsToday = flowOf(PagingData.from(historyToday))
    val itemsYesterday = flowOf(PagingData.from(historyYesterday))
    val itemsThisWeek = flowOf(PagingData.from(historyThisWeek))
    val itemsBeforeThisWeek = flowOf(PagingData.from(historyBeforeThisWeek))

    val historyUIModel = HistoryUIModel(SharedPreferencesModel(LocalContext.current))

    NeevaTheme {
        HistoryUI(
            historyUIModel = historyUIModel,
            historyToday = itemsToday.collectAsLazyPagingItems(),
            historyYesterday = itemsYesterday.collectAsLazyPagingItems(),
            historyThisWeek = itemsThisWeek.collectAsLazyPagingItems(),
            historyBeforeThisWeek = itemsBeforeThisWeek.collectAsLazyPagingItems(),
            onClearHistory = {},
            onClose = {},
            onOpenUrl = {},
            onDeleteVisit = { _, _ -> },
            faviconCache = mockFaviconCache,
            domainProvider = mockFaviconCache.domainProvider
        )
    }
}
