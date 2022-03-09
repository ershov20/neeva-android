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
    selectedScreen: SelectedScreen,
    cardGridModel: CardGridModel,
    modifier: Modifier = Modifier
) {
    // Reset the scroll state of the LazyVerticalGrid every time the active tab changes.
    val tabs: List<TabInfo> by browserWrapper.orderedTabList.collectAsState()
    val activeTabIndex: Int = tabs.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    val gridState = LazyGridState(activeTabIndex)

    CardGrid(
        selectedScreen = selectedScreen,
        gridState = gridState,
        cardGridModel = cardGridModel,
        tabs = tabs,
        faviconCache = browserWrapper.faviconCache,
        screenshotProvider = browserWrapper.tabScreenshotManager::restoreScreenshot,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardGrid(
    selectedScreen: SelectedScreen,
    gridState: LazyGridState,
    cardGridModel: CardGridModel,
    tabs: List<TabInfo>,
    faviconCache: FaviconCache,
    screenshotProvider: (tabId: String) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    // TODO(dan.alcantara): Material3 doesn't seem to have a MaterialTheme.colors.isLight function.
    val emptyLogoId = if (isSystemInDarkTheme()) {
        when (selectedScreen) {
            SelectedScreen.REGULAR_TABS -> R.drawable.ic_empty_regular_tabs_dark
            SelectedScreen.INCOGNITO_TABS -> R.drawable.ic_empty_incognito_tabs_dark
            else -> TODO("Not implemented")
        }
    } else {
        when (selectedScreen) {
            SelectedScreen.REGULAR_TABS -> R.drawable.ic_empty_regular_tabs_light
            SelectedScreen.INCOGNITO_TABS -> R.drawable.ic_empty_incognito_tabs_light
            else -> TODO("Not implemented")
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
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
                        text = stringResource(
                            when (selectedScreen) {
                                SelectedScreen.REGULAR_TABS -> R.string.empty_regular_tabs_title
                                SelectedScreen.INCOGNITO_TABS -> R.string.empty_incognito_tabs_title
                                else -> TODO("Not implemented")
                            }
                        ),
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
                        onSelect = { cardGridModel.selectTab(tab) },
                        onClose = { cardGridModel.closeTab(tab) },
                        faviconCache = faviconCache,
                        screenshotProvider = screenshotProvider
                    )
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
    @Preview("RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun CardGrid_Preview(@PreviewParameter(CardGridPreviews::class) params: Params) {
        val darkTheme = params.darkTheme
        val selectedScreen = if (params.isIncognito) {
            SelectedScreen.INCOGNITO_TABS
        } else {
            SelectedScreen.REGULAR_TABS
        }

        NeevaTheme(useDarkTheme = darkTheme) {
            val gridState = rememberLazyGridState()
            val cardGridContainerModel = mockCardGridContainerModel

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
                selectedScreen = selectedScreen,
                gridState = gridState,
                cardGridModel = cardGridContainerModel,
                tabs = tabs,
                faviconCache = mockFaviconCache,
                screenshotProvider = { null }
            )
        }
    }
}
