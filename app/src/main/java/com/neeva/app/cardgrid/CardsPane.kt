package com.neeva.app.cardgrid

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.neeva.app.LocalCardsPaneModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.spaces.SpacesGridBottomBar
import com.neeva.app.cardgrid.tabs.TabGridBottomBar

enum class SelectedScreen {
    INCOGNITO_TABS, REGULAR_TABS, SPACES
}

@Composable
fun CardsPane(webLayerModel: WebLayerModel) {
    val cardsPaneModel = LocalCardsPaneModel.current

    val currentBrowser by webLayerModel.currentBrowserFlow.collectAsState()
    val hasNoTabs = currentBrowser.hasNoTabsFlow().collectAsState(false)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModeSwitcher(
                selectedScreen = cardsPaneModel.selectedScreen,
                onSwitchScreen = cardsPaneModel::switchScreen
            )

            CardGridContainer(
                webLayerModel = webLayerModel,
                cardsPaneModel = cardsPaneModel,
                previousScreen = cardsPaneModel.previousScreen.value,
                selectedScreen = cardsPaneModel.selectedScreen.value,
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            )

            when (cardsPaneModel.selectedScreen.value) {
                SelectedScreen.SPACES -> SpacesGridBottomBar(
                    isDoneEnabled = !hasNoTabs.value,
                    onNavigateToSpacesWebsite = cardsPaneModel::showSpacesWebsite,
                    onDone = cardsPaneModel::showBrowser
                )

                else -> TabGridBottomBar(
                    isIncognito = currentBrowser.isIncognito,
                    hasNoTabs = hasNoTabs.value,
                    onCloseAllTabs = { cardsPaneModel.closeAllTabs(currentBrowser) },
                    onOpenLazyTab = { cardsPaneModel.openLazyTab(currentBrowser) },
                    onDone = cardsPaneModel::showBrowser
                )
            }
        }
    }
}
