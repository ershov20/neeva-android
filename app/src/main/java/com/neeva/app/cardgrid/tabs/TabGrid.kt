package com.neeva.app.cardgrid.tabs

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.cardgrid.CardGrid
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.history.HistoryHeader
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPrefFolder.App.AutomaticallyArchiveTabs
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.previewFaviconCache
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LandscapePreviewsDark
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.previewCardGridTitles
import com.neeva.app.ui.theme.Dimensions
import java.util.concurrent.TimeUnit

@Composable
fun TabGrid(
    browserWrapper: BrowserWrapper,
    cardsPaneModel: CardsPaneModel,
    modifier: Modifier = Modifier
) {
    val tabs: List<TabInfo> by browserWrapper.orderedTabList.collectAsState()
    val isArchiveEnabled by LocalSettingsDataModel.current
        .getToggleState(SettingsToggle.AUTOMATED_TAB_MANAGEMENT)

    TabGrid(
        isIncognito = browserWrapper.isIncognito,
        isAutomatedTabManagementEnabled = isArchiveEnabled,
        onSelectTab = { tabInfo -> cardsPaneModel.selectTab(browserWrapper, tabInfo) },
        onCloseTabs = { tabInfo -> cardsPaneModel.closeTab(browserWrapper, tabInfo) },
        onShowArchivedTabs = { cardsPaneModel.showArchivedTabs(browserWrapper) },
        tabs = tabs,
        faviconCache = browserWrapper.faviconCache,
        screenshotProvider = browserWrapper::restoreScreenshotOfTab,
        modifier = modifier
    )
}

@Composable
fun TabGrid(
    isIncognito: Boolean,
    isAutomatedTabManagementEnabled: Boolean,
    onSelectTab: (TabInfo) -> Unit,
    onCloseTabs: (TabInfo) -> Unit,
    onShowArchivedTabs: () -> Unit,
    tabs: List<TabInfo>,
    faviconCache: FaviconCache,
    screenshotProvider: suspend (tabId: String) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    val visibleTabs: List<TabInfo> by remember(tabs) {
        derivedStateOf {
            tabs.filterNot { it.isClosing }
        }
    }

    if (isAutomatedTabManagementEnabled) {
        ChronologicalTabGrid(
            isIncognito = isIncognito,
            onSelectTab = onSelectTab,
            onCloseTabs = onCloseTabs,
            onShowArchivedTabs = onShowArchivedTabs,
            visibleTabs = visibleTabs,
            faviconCache = faviconCache,
            screenshotProvider = screenshotProvider,
            modifier = modifier
        )
        return
    }

    CardGrid(
        items = visibleTabs,
        modifier = modifier,
        computeFirstVisibleItemIndex = {
            // Scroll the user to the currently selected tab when they first navigate here,
            // but don't update it if the user closes tabs while they're here.
            visibleTabs.indexOfFirst { it.isSelected }.coerceAtLeast(0)
        },
        emptyComposable = {
            TabGridEmptyState(isIncognito, Modifier.fillMaxSize())
        }
    ) { _, listItems ->
        items(
            items = listItems,
            key = { it.id }
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
}

@Composable
fun ChronologicalTabGrid(
    isIncognito: Boolean,
    onSelectTab: (TabInfo) -> Unit,
    onCloseTabs: (TabInfo) -> Unit,
    onShowArchivedTabs: () -> Unit,
    visibleTabs: List<TabInfo>,
    faviconCache: FaviconCache,
    screenshotProvider: suspend (tabId: String) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val archiveAfterOption = AutomaticallyArchiveTabs
        .getFlow(LocalSharedPreferencesModel.current)
        .collectAsState()

    val sections by remember(visibleTabs, archiveAfterOption) {
        derivedStateOf {
            computeTabGridSections(context, visibleTabs, archiveAfterOption.value)
        }
    }

    Column(modifier = modifier) {
        CardGrid(
            items = sections,
            modifier = Modifier.weight(1.0f),
            computeFirstVisibleItemIndex = { numCellsPerRow ->
                computeVisibleItemIndex(sections, numCellsPerRow)
            },
            emptyComposable = {
                TabGridEmptyState(isIncognito, Modifier.fillMaxSize())
            }
        ) { numCellsPerRow, listItems ->
            listItems.forEach { section ->
                item(span = { GridItemSpan(numCellsPerRow) }) {
                    HistoryHeader(section.header)
                }

                items(
                    items = section.items,
                    key = { it.id }
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
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = Dimensions.PADDING_LARGE,
                vertical = Dimensions.PADDING_SMALL
            )
        ) {
            FilledTonalButton(
                onClick = onShowArchivedTabs,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.size(Dimensions.PADDING_SMALL))

                Text(stringResource(R.string.archived_tabs))
            }
        }
    }
}

fun computeVisibleItemIndex(sections: List<TabGridSection<TabInfo>>, numCells: Int): Int {
    // Only look in the first section for the selected tab because selected tabs automatically
    // count as being active today.
    return sections.firstOrNull()
        ?.items
        ?.indexOfFirst { it.isSelected }
        ?.let {
            if (it < numCells) {
                // If we're showing the first row of the top section, show the section header.
                0
            } else {
                // Scroll down directly to the item.
                it + 1
            }
        } ?: 0
}

class TabGridPreviews : BooleanPreviewParameterProvider<TabGridPreviews.Params>(2) {
    data class Params(
        val isIncognito: Boolean,
        val isAutomatedTabManagementEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        isIncognito = booleanArray[0],
        isAutomatedTabManagementEnabled = booleanArray[1]
    )

    @PortraitPreviews
    @LandscapePreviews
    @Composable
    fun TabGrid_Preview_Light(@PreviewParameter(TabGridPreviews::class) params: Params) {
        TabGridPreview(darkTheme = false, params = params)
    }

    @PortraitPreviewsDark
    @LandscapePreviewsDark
    @Composable
    fun TabGrid_Preview_Dark(@PreviewParameter(TabGridPreviews::class) params: Params) {
        TabGridPreview(darkTheme = true, params = params)
    }

    @Composable
    fun TabGridPreview(darkTheme: Boolean, params: Params) {
        NeevaThemePreviewContainer(
            useDarkTheme = darkTheme,
            addBorder = false
        ) {
            val tabs = mutableListOf<TabInfo>()

            val selectedTabIndex = 5
            val tabTitles = previewCardGridTitles
            val now = System.currentTimeMillis()
            tabTitles.forEachIndexed { i, title ->
                tabs.add(
                    TabInfo(
                        id = "tab $i",
                        url = Uri.parse("https://www.neeva.com/$i"),
                        title = title,
                        isSelected = i == selectedTabIndex,
                        data = TabInfo.PersistedData(
                            lastActiveMs = now - TimeUnit.DAYS.toMillis(i.toLong())
                        )
                    )
                )
            }

            TabGrid(
                isIncognito = params.isIncognito,
                isAutomatedTabManagementEnabled = params.isAutomatedTabManagementEnabled,
                onSelectTab = {},
                onCloseTabs = {},
                onShowArchivedTabs = {},
                tabs = tabs,
                faviconCache = previewFaviconCache,
                screenshotProvider = { null }
            )
        }
    }
}
