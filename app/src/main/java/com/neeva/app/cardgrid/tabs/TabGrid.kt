package com.neeva.app.cardgrid.tabs

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LandscapePreviewsDark
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.previewCardGridTitles
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.HeavyDivider
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
    val displayTabsByReverseCreationTime = LocalSettingsDataModel.current
        .getToggleState(SettingsToggle.DEBUG_ENABLE_DISPLAY_TABS_BY_REVERSE_CREATION_TIME)

    val sections by remember(visibleTabs, archiveAfterOption, displayTabsByReverseCreationTime) {
        derivedStateOf {
            computeTabGridSections(
                context = context,
                tabs = visibleTabs,
                archiveAfterOption = archiveAfterOption.value,
                displayTabsInReverseCreationTime = displayTabsByReverseCreationTime.value
            )
        }
    }

    CardGrid(
        items = sections,
        computeFirstVisibleItemIndex = { numCellsPerRow ->
            computeVisibleItemIndex(sections, numCellsPerRow)
        },
        emptyComposable = {
            Column {
                TabGridEmptyState(isIncognito, Modifier.weight(1.0f))

                ArchivedTabsButton(onShowArchivedTabs = onShowArchivedTabs)
            }
        },
        modifier = modifier
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

        item(span = { GridItemSpan(numCellsPerRow) }) {
            Column(Modifier.fillMaxHeight()) {
                HeavyDivider()
                ArchivedTabsButton(onShowArchivedTabs = onShowArchivedTabs)
            }
        }
    }
}

@Composable
fun ArchivedTabsButton(onShowArchivedTabs: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onShowArchivedTabs,
            modifier = Modifier.padding(
                horizontal = Dimensions.PADDING_LARGE,
                vertical = Dimensions.PADDING_SMALL
            )
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null
            )

            Spacer(modifier = Modifier.size(Dimensions.PADDING_SMALL))

            Text(stringResource(R.string.archived_tabs))
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

@PortraitPreviews
@LandscapePreviews
@Composable
fun TabGridPreview_LightIncognitoArchiving() {
    TabGridPreview(
        darkTheme = false,
        isIncognito = true,
        isAutomatedTabManagementEnabled = true
    )
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun TabGridPreview_LightIncognitoLegacy() {
    TabGridPreview(
        darkTheme = false,
        isIncognito = true,
        isAutomatedTabManagementEnabled = false
    )
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun TabGridPreview_LightRegularArchiving() {
    TabGridPreview(
        darkTheme = false,
        isIncognito = false,
        isAutomatedTabManagementEnabled = true
    )
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun TabGridPreview_LightRegularArchivingWithoutTabs() {
    TabGridPreview(
        darkTheme = false,
        isIncognito = false,
        isAutomatedTabManagementEnabled = true,
        tabTitles = emptyList()
    )
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun TabGridPreview_LightRegularLegacy() {
    TabGridPreview(
        darkTheme = false,
        isIncognito = false,
        isAutomatedTabManagementEnabled = false
    )
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun TabGridPreview_DarkIncognitoArchiving() {
    TabGridPreview(
        darkTheme = true,
        isIncognito = true,
        isAutomatedTabManagementEnabled = true
    )
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun TabGridPreview_DarkIncognitoLegacy() {
    TabGridPreview(
        darkTheme = true,
        isIncognito = true,
        isAutomatedTabManagementEnabled = false
    )
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun TabGridPreview_DarkRegularArchiving() {
    TabGridPreview(
        darkTheme = true,
        isIncognito = false,
        isAutomatedTabManagementEnabled = true
    )
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun TabGridPreview_DarkRegularLegacy() {
    TabGridPreview(
        darkTheme = true,
        isIncognito = false,
        isAutomatedTabManagementEnabled = false
    )
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun TabGridPreview_DarkRegularArchivingWithoutTabs() {
    TabGridPreview(
        darkTheme = true,
        isIncognito = false,
        isAutomatedTabManagementEnabled = true,
        tabTitles = emptyList()
    )
}

@Composable
private fun TabGridPreview(
    darkTheme: Boolean,
    isIncognito: Boolean,
    isAutomatedTabManagementEnabled: Boolean,
    tabTitles: List<String> = previewCardGridTitles
) {
    NeevaThemePreviewContainer(
        useDarkTheme = darkTheme,
        addBorder = false
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val tabs = mutableListOf<TabInfo>()

            val selectedTabIndex = 5
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
                isIncognito = isIncognito,
                isAutomatedTabManagementEnabled = isAutomatedTabManagementEnabled,
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
