package com.neeva.app.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.R
import com.neeva.app.settings.SettingsMain
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.storage.Visit
import com.neeva.app.suggestions.NavSuggestView
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.web.WebLayerModel
import com.neeva.app.widgets.CollapsingState
import com.neeva.app.widgets.collapsibleHeaderItems


@OptIn(
    ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun HistoryContainer(
    appNavModel: AppNavModel,
    historyViewModel: HistoryViewModel,
    domainViewModel: DomainViewModel
) {
    val state: AppNavState by appNavModel.state.observeAsState(AppNavState.HIDDEN)
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = state == AppNavState.HISTORY,
        enter = slideInHorizontally(
            initialOffsetX = { with(density) { 600.dp.roundToPx() } },
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { with(density) { 600.dp.roundToPx() } }
        )
    ) {
        HistoryUI(
            appNavModel = appNavModel,
            historyViewModel = historyViewModel,
            domainViewModel = domainViewModel
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryUI(appNavModel: AppNavModel, historyViewModel: HistoryViewModel, domainViewModel: DomainViewModel) {
    val historyToday: List<Pair<NavSuggestion, Visit>> by historyViewModel.historyToday.observeAsState(
        emptyList())
    val historyYesterday: List<Pair<NavSuggestion, Visit>> by historyViewModel.historyYesterday.observeAsState(
        emptyList())
    val historyThisWeek: List<Pair<NavSuggestion, Visit>> by historyViewModel.historyThisWeek.observeAsState(
        emptyList())

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        stickyHeader {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colors.primary),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = "Close History",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier
                        .size(48.dp, 48.dp)
                        .clickable { appNavModel.setContentState(AppNavState.HIDDEN) },
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
                )
                Text(
                    text = "History",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onPrimary,
                    maxLines = 1,
                )
            }
        }

        collapsibleHeaderItems("Today", CollapsingState.SHOW_COMPACT, items = historyToday) {
            NavSuggestView(
                faviconData = domainViewModel.getFaviconFor(it.first.url),
                onOpenUrl = { uri ->
                    appNavModel.onOpenUrl(uri)
                    appNavModel.setContentState(AppNavState.HIDDEN)
                },
                navSuggestion = it.first
            )
        }

        collapsibleHeaderItems("Yesterday", CollapsingState.SHOW_COMPACT, items = historyYesterday) {
            NavSuggestView(
                faviconData = domainViewModel.getFaviconFor(it.first.url),
                onOpenUrl = { uri ->
                    appNavModel.onOpenUrl(uri)
                    appNavModel.setContentState(AppNavState.HIDDEN)
                },
                navSuggestion = it.first
            )
        }

        collapsibleHeaderItems("This Week", CollapsingState.SHOW_COMPACT, items = historyThisWeek) {
            NavSuggestView(
                faviconData = domainViewModel.getFaviconFor(it.first.url),
                onOpenUrl = { uri ->
                    appNavModel.onOpenUrl(uri)
                    appNavModel.setContentState(AppNavState.HIDDEN)
                },
                navSuggestion = it.first
            )
        }
    }
}