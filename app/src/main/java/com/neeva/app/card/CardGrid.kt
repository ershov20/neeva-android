package com.neeva.app.card

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.mockFaviconCache
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardGrid(
    browserWrapper: BrowserWrapper,
    cardGridModel: CardGridModel,
    modifier: Modifier = Modifier
) {
    // Reset the scroll state of the LazyVerticalGrid every time the active tab changes.
    val tabs: List<TabInfo> by browserWrapper.orderedTabList.collectAsState()
    val activeTabIndex: Int = tabs.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    val gridState = LazyGridState(activeTabIndex)

    CardGrid(
        isIncognito = browserWrapper.isIncognito,
        gridState = gridState,
        onSelect = { tabInfo -> cardGridModel.selectTab(browserWrapper, tabInfo) },
        onClose = { tabInfo -> cardGridModel.closeTab(browserWrapper, tabInfo) },
        tabs = tabs,
        faviconCache = browserWrapper.faviconCache,
        screenshotProvider = browserWrapper.tabScreenshotManager::restoreScreenshot,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardGrid(
    isIncognito: Boolean,
    gridState: LazyGridState,
    onSelect: (TabInfo) -> Unit,
    onClose: (TabInfo) -> Unit,
    tabs: List<TabInfo>,
    faviconCache: FaviconCache,
    screenshotProvider: (tabId: String) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    val emptyLogoId: Int
    val emptyStringId: Int
    if (isIncognito) {
        // TODO(dan.alcantara): Material3 doesn't seem to have a MaterialTheme.colors.isLight().
        emptyLogoId = if (isSystemInDarkTheme()) {
            R.drawable.ic_empty_incognito_tabs_dark
        } else {
            R.drawable.ic_empty_incognito_tabs_light
        }
        emptyStringId = R.string.empty_incognito_tabs_title
    } else {
        emptyLogoId = if (isSystemInDarkTheme()) {
            R.drawable.ic_empty_regular_tabs_dark
        } else {
            R.drawable.ic_empty_regular_tabs_light
        }
        emptyStringId = R.string.empty_regular_tabs_title
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            val contentModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            if (tabs.isEmpty()) {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(emptyLogoId),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(emptyStringId),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(R.string.empty_tab_hint),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    cells = GridCells.Fixed(2),
                    state = gridState,
                    modifier = contentModifier
                ) {
                    items(tabs) { tab ->
                        TabCard(
                            tabInfo = tab,
                            onSelect = { onSelect(tab) },
                            onClose = { onClose(tab) },
                            faviconCache = faviconCache,
                            screenshotProvider = screenshotProvider
                        )
                    }
                }
            }
        }
    }
}

class CardGridPreviews : BooleanPreviewParameterProvider<CardGridPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val isIncognito: Boolean,
        val emptyTabList: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isIncognito = booleanArray[1],
        emptyTabList = booleanArray[2]
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Preview("1x", locale = "en")
    @Preview("2x", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x", locale = "he")
    @Composable
    fun CardGrid_Preview(@PreviewParameter(CardGridPreviews::class) params: Params) {
        val darkTheme = params.darkTheme

        NeevaTheme(useDarkTheme = darkTheme) {
            val gridState = rememberLazyGridState()
            val tabs = mutableListOf<TabInfo>()
            if (!params.emptyTabList) {
                val selectedTabIndex = 5
                val tabTitles = listOf(
                    stringResource(id = R.string.debug_long_string_primary),
                    "short",
                    "Amazon.com",
                    "Ad-free, private search",
                    "Some other amazing site",
                    "Yep, another site",
                    "Drink more Ovaltine"
                )
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
            }

            CardGrid(
                isIncognito = params.isIncognito,
                gridState = gridState,
                onSelect = {},
                onClose = {},
                tabs = tabs,
                faviconCache = mockFaviconCache,
                screenshotProvider = { null }
            )
        }
    }
}
