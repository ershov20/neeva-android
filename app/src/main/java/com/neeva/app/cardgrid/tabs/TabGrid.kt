package com.neeva.app.cardgrid.tabs

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.cardgrid.CardGrid
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.previewCardGridTitles
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun TabGrid(
    browserWrapper: BrowserWrapper,
    cardsPaneModel: CardsPaneModel,
    modifier: Modifier = Modifier
) {
    // Scroll the user to the currently selected tab when they first navigate here, but don't
    // update it if the user closes tabs while they're here.
    val tabs: List<TabInfo> by browserWrapper.orderedTabList.collectAsState()
    val activeTabIndex: Int = tabs.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = activeTabIndex)

    val visibleTabs = remember {
        derivedStateOf {
            tabs.filterNot { it.isClosing }
        }
    }

    TabGrid(
        isIncognito = browserWrapper.isIncognito,
        gridState = gridState,
        onSelectTab = { tabInfo -> cardsPaneModel.selectTab(browserWrapper, tabInfo) },
        onCloseTabs = { tabInfo -> cardsPaneModel.closeTab(browserWrapper, tabInfo) },
        tabs = visibleTabs.value,
        faviconCache = browserWrapper.faviconCache,
        screenshotProvider = browserWrapper::restoreScreenshotOfTab,
        modifier = modifier
    )
}

@Composable
fun TabGrid(
    isIncognito: Boolean,
    gridState: LazyGridState,
    onSelectTab: (TabInfo) -> Unit,
    onCloseTabs: (TabInfo) -> Unit,
    tabs: List<TabInfo>,
    faviconCache: FaviconCache,
    screenshotProvider: suspend (tabId: String) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    CardGrid(
        gridState = gridState,
        allItems = tabs,
        modifier = modifier,
        emptyComposable = { TabGridEmptyState(isIncognito, Modifier.fillMaxSize()) }
    ) { tab ->
        TabCard(
            tabInfo = tab,
            onSelect = { onSelectTab(tab) },
            onClose = { onCloseTabs(tab) },
            faviconCache = faviconCache,
            screenshotProvider = screenshotProvider
        )
    }
}

class TabGridPreviews : BooleanPreviewParameterProvider<TabGridPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val isIncognito: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isIncognito = booleanArray[1]
    )

    @Preview("1x", locale = "en")
    @Preview("2x", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x", locale = "he")
    @Preview("Landscape, 1x scale", widthDp = 1024, heightDp = 600, locale = "en")
    @Composable
    fun TabGrid_Preview(@PreviewParameter(TabGridPreviews::class) params: Params) {
        val darkTheme = params.darkTheme

        NeevaTheme(useDarkTheme = darkTheme) {
            val gridState = rememberLazyGridState()
            val tabs = mutableListOf<TabInfo>()

            val selectedTabIndex = 5
            val tabTitles = previewCardGridTitles
            tabTitles.forEachIndexed { i, title ->
                tabs.add(
                    TabInfo(
                        id = "tab $i",
                        url = Uri.parse("https://www.neeva.com/$i"),
                        title = title,
                        isSelected = i == selectedTabIndex
                    )
                )
            }

            TabGrid(
                isIncognito = params.isIncognito,
                gridState = gridState,
                onSelectTab = {},
                onCloseTabs = {},
                tabs = tabs,
                faviconCache = mockFaviconCache,
                screenshotProvider = { null }
            )
        }
    }
}
