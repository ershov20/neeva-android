package com.neeva.app.history

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.neeva.app.R
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.ComposableSingletonEntryPoint
import com.neeva.app.widgets.collapsibleSection
import dagger.hilt.EntryPoints
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import kotlinx.coroutines.flow.flowOf

@Composable
fun HistoryUI(
    onClose: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    faviconCache: FaviconCache,
    now: LocalDate = LocalDate.now()
) {
    val entryPoint = EntryPoints.get(
        LocalContext.current.applicationContext,
        ComposableSingletonEntryPoint::class.java
    )
    val historyManager = entryPoint.historyManager()
    val domainProvider = entryPoint.domainProvider()

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

    HistoryUI(
        historyToday = historyToday,
        historyYesterday = historyYesterday,
        historyBefore = historyThisWeek,
        onClose = onClose,
        onOpenUrl = onOpenUrl,
        faviconCache = faviconCache,
        domainProvider = domainProvider
    )
}

@Composable
fun HistoryUI(
    historyToday: LazyPagingItems<Site>,
    historyYesterday: LazyPagingItems<Site>,
    historyBefore: LazyPagingItems<Site>,
    onClose: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider
) {
    val isTodayDisplayed = remember { mutableStateOf(true) }
    val isYesterdayDisplayed = remember { mutableStateOf(true) }
    val isThisWeekDisplayed = remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.history),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            backgroundColor = MaterialTheme.colorScheme.surface,
            navigationIcon = {
                IconButton(
                    onClick = { onClose() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.close),
                        contentScale = ContentScale.Inside,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            collapsibleSection(
                label = R.string.history_today,
                displayedItems = historyToday,
                isExpanded = isTodayDisplayed
            ) { site ->
                site?.let {
                    NavSuggestion(faviconCache, onOpenUrl, site.toNavSuggestion(domainProvider))
                }
            }

            collapsibleSection(
                label = R.string.history_yesterday,
                displayedItems = historyYesterday,
                isExpanded = isYesterdayDisplayed
            ) { site ->
                site?.let {
                    NavSuggestion(faviconCache, onOpenUrl, site.toNavSuggestion(domainProvider))
                }
            }

            collapsibleSection(
                label = R.string.history_this_week,
                displayedItems = historyBefore,
                isExpanded = isThisWeekDisplayed
            ) { site ->
                site?.let {
                    NavSuggestion(faviconCache, onOpenUrl, site.toNavSuggestion(domainProvider))
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

    val itemsToday = flowOf(PagingData.from(historyToday))
    val itemsYesterday = flowOf(PagingData.from(historyYesterday))
    val itemsThisWeek = flowOf(PagingData.from(historyThisWeek))

    NeevaTheme {
        HistoryUI(
            historyToday = itemsToday.collectAsLazyPagingItems(),
            historyYesterday = itemsYesterday.collectAsLazyPagingItems(),
            historyBefore = itemsThisWeek.collectAsLazyPagingItems(),
            onClose = {},
            onOpenUrl = {},
            faviconCache = mockFaviconCache,
            domainProvider = mockFaviconCache.domainProvider
        )
    }
}
