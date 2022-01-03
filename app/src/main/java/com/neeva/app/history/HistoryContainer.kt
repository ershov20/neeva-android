package com.neeva.app.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.storage.Site

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HistoryContainer(
    appNavModel: AppNavModel,
    historyViewModel: HistoryViewModel
) {
    val state: AppNavState by appNavModel.state.collectAsState()
    val history: List<Site> by historyViewModel.historyWithinRange.collectAsState(emptyList())

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
            history = history,
            onClose = appNavModel::showBrowser,
            onOpenUrl = appNavModel::openUrl,
            faviconProvider = historyViewModel::getFaviconFlow
        )
    }
}
