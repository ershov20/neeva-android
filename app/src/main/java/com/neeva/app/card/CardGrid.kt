package com.neeva.app.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.R
import com.neeva.app.browsing.TabInfo
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.storage.Favicon
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.Button
import org.chromium.weblayer.Tab

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CardsContainer(
    appNavModel: AppNavModel,
    webLayerModel: WebLayerModel,
    historyManager: HistoryManager,
    urlBarModel: URLBarModel
) {
    val state: AppNavState by appNavModel.state.collectAsState()

    AnimatedVisibility(
        visible = state == AppNavState.CARD_GRID,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Reset the scroll state of the LazyVerticalGrid every time the active tab changes.
        // TODO(dan.alcantara): We'll need to investigate how this should work with tab groups
        //                      and child tabs.
        val activeTab: Tab? by webLayerModel.activeTabModel.activeTabFlow.collectAsState()
        val activeTabIndex = activeTab?.guid
            ?.let { guid -> webLayerModel.orderedTabList.value.indexOfFirst { it.id == guid } }
            ?.coerceAtLeast(0)
            ?: 0
        val listState = LazyListState(activeTabIndex)

        CardGrid(webLayerModel, historyManager, appNavModel, urlBarModel, listState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardGrid(
    webLayerModel: WebLayerModel,
    historyManager: HistoryManager,
    appNavModel: AppNavModel,
    urlBarModel: URLBarModel,
    listState: LazyListState
) {
    val tabs: List<TabInfo> by webLayerModel.orderedTabList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(tabs) { tab ->
                val favicon: Favicon? by historyManager.getFaviconFlow(tab.url).collectAsState(null)
                val bitmap = favicon?.toBitmap()

                TabCard(
                    tab = tab,
                    faviconData = bitmap,
                    onSelect = {
                        webLayerModel.selectTab(tab)
                        appNavModel.showBrowser()
                    },
                    onClose = { webLayerModel.closeTab(tab) }
                )
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.LightGray))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(enabled = true, resID = R.drawable.ic_baseline_add_24, contentDescription = "New Tab") {
                urlBarModel.openLazyTab()
                appNavModel.showBrowser()
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(enabled = true, resID = R.drawable.ic_baseline_close_24, contentDescription = "Done") {
                appNavModel.showBrowser()
            }
        }
    }
}

