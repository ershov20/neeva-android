package com.neeva.app.history

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.SuggestionType
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.BrandedTextButton
import com.neeva.app.widgets.collapsible.CollapsingSectionState
import com.neeva.app.widgets.collapsible.collapsibleSection
import com.neeva.app.widgets.collapsible.setNextState
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import kotlinx.coroutines.flow.flowOf

@Composable
fun HistoryUI(
    onClose: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    onTapSuggestion: ((SuggestionType, Int?) -> Unit)? = null,
    faviconCache: FaviconCache,
    now: LocalDate = LocalDate.now()
) {
    val domainProvider = LocalEnvironment.current.domainProvider
    val historyManager = LocalEnvironment.current.historyManager

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
        historyToday = historyToday,
        historyYesterday = historyYesterday,
        historyThisWeek = historyThisWeek,
        historyBeforeThisWeek = historyBeforeThisWeek,
        onClearHistory = onClearHistory,
        onClose = onClose,
        onOpenUrl = onOpenUrl,
        onTapSuggestion = onTapSuggestion,
        faviconCache = faviconCache,
        domainProvider = domainProvider
    )
}

@Composable
fun HistoryUI(
    historyToday: LazyPagingItems<Site>,
    historyYesterday: LazyPagingItems<Site>,
    historyThisWeek: LazyPagingItems<Site>,
    historyBeforeThisWeek: LazyPagingItems<Site>,
    onClose: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    onTapSuggestion: ((SuggestionType, Int?) -> Unit)? = null,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider
) {
    val isTodayDisplayed = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }
    val isYesterdayDisplayed = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }
    val isThisWeekDisplayed = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }
    val isBeforeThisWeekDisplayed = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }

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
                BrandedTextButton(
                    enabled = true,
                    stringResID = R.string.settings_clear_browsing_data,
                    onClick = onClearHistory
                )
            }

            collapsibleSection(
                label = R.string.history_today,
                collapsingSectionState = isTodayDisplayed,
                updateCollapsingHeaderState = isTodayDisplayed::setNextState,
            ) {
                items(historyToday) { site ->
                    site?.let {
                        NavSuggestion(
                            faviconCache,
                            onOpenUrl,
                            onTapSuggestion,
                            site.toNavSuggestion(domainProvider)
                        )
                    }
                }
            }

            collapsibleSection(
                label = R.string.history_yesterday,
                collapsingSectionState = isYesterdayDisplayed,
                updateCollapsingHeaderState = isYesterdayDisplayed::setNextState
            ) {
                items(historyYesterday) { site ->
                    site?.let {
                        NavSuggestion(
                            faviconCache,
                            onOpenUrl,
                            onTapSuggestion,
                            site.toNavSuggestion(domainProvider)
                        )
                    }
                }
            }

            collapsibleSection(
                label = R.string.history_this_week,
                collapsingSectionState = isThisWeekDisplayed,
                updateCollapsingHeaderState = isThisWeekDisplayed::setNextState
            ) {
                items(historyThisWeek) { site ->
                    site?.let {
                        NavSuggestion(
                            faviconCache,
                            onOpenUrl,
                            onTapSuggestion,
                            site.toNavSuggestion(domainProvider)
                        )
                    }
                }
            }

            collapsibleSection(
                label = R.string.history_earlier,
                collapsingSectionState = isBeforeThisWeekDisplayed,
                updateCollapsingHeaderState = isBeforeThisWeekDisplayed::setNextState
            ) {
                items(historyBeforeThisWeek) { site ->
                    site?.let {
                        NavSuggestion(
                            faviconCache,
                            onOpenUrl,
                            onTapSuggestion,
                            site.toNavSuggestion(domainProvider)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HistoryUI_Preview() {
    var ids = 0

    // Add items for today.
    val historyToday = mutableListOf<Site>()
    for (i in 0 until 2) {
        historyToday.add(
            Site(
                siteUID = ids++,
                siteURL = "https://www.site$ids.com/$i",
                title = null,
                largestFavicon = null
            )
        )
    }

    // Add items for yesterday.
    val historyYesterday = mutableListOf<Site>()
    for (i in 0 until 2) {
        historyYesterday.add(
            Site(
                siteUID = ids++,
                siteURL = "https://www.site$ids.com/$i",
                title = null,
                largestFavicon = null
            )
        )
    }

    // Add one item for each day before that.  Items that are too old should not be displayed.
    val historyThisWeek = mutableListOf<Site>()
    for (daysAgo in 2 until 10) {
        historyThisWeek.add(
            Site(
                siteUID = ids++,
                siteURL = "https://www.site$ids.com/${daysAgo}_days_ago",
                title = null,
                largestFavicon = null
            )
        )
    }

    // Add one item for each day before this week.
    val historyBeforeThisWeek = mutableListOf<Site>()
    for (daysAgo in 10 until 25) {
        historyBeforeThisWeek.add(
            Site(
                siteUID = ids++,
                siteURL = "https://www.site$ids.com/${daysAgo}_days_ago",
                title = null,
                largestFavicon = null
            )
        )
    }

    val itemsToday = flowOf(PagingData.from(historyToday))
    val itemsYesterday = flowOf(PagingData.from(historyYesterday))
    val itemsThisWeek = flowOf(PagingData.from(historyThisWeek))
    val itemsBeforeThisWeek = flowOf(PagingData.from(historyBeforeThisWeek))

    NeevaTheme {
        HistoryUI(
            historyToday = itemsToday.collectAsLazyPagingItems(),
            historyYesterday = itemsYesterday.collectAsLazyPagingItems(),
            historyThisWeek = itemsThisWeek.collectAsLazyPagingItems(),
            historyBeforeThisWeek = itemsBeforeThisWeek.collectAsLazyPagingItems(),
            onClearHistory = {},
            onClose = {},
            onOpenUrl = {},
            faviconCache = mockFaviconCache,
            domainProvider = mockFaviconCache.domainProvider
        )
    }
}
