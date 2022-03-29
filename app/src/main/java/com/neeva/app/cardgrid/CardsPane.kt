package com.neeva.app.cardgrid

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.neeva.app.LocalAppNavModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.spaces.SpacesGridBottomBar
import com.neeva.app.cardgrid.tabs.TabGridBottomBar

enum class SelectedScreen {
    INCOGNITO_TABS, REGULAR_TABS, SPACES
}

@Composable
fun CardsPane(webLayerModel: WebLayerModel) {
    val appNavModel = LocalAppNavModel.current

    val cardsPaneModel = remember(webLayerModel, appNavModel) {
        CardsPaneModelImpl(webLayerModel, appNavModel)
    }

    val currentBrowser by webLayerModel.currentBrowserFlow.collectAsState()
    val hasNoTabs = currentBrowser.hasNoTabsFlow().collectAsState(false)

    // Keep track of what screen is currently being viewed by the user.
    val selectedScreen: MutableState<SelectedScreen> = remember {
        mutableStateOf(
            if (currentBrowser.isIncognito) {
                SelectedScreen.INCOGNITO_TABS
            } else {
                SelectedScreen.REGULAR_TABS
            }
        )
    }

    val previousScreen: MutableState<SelectedScreen?> = remember {
        mutableStateOf(null)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModeSwitcher(
                selectedScreen = selectedScreen,
                onSwitchScreen = {
                    cardsPaneModel.switchScreen(it)
                    previousScreen.value = selectedScreen.value
                    selectedScreen.value = it
                }
            )

            CardGridContainer(
                webLayerModel = webLayerModel,
                cardsPaneModel = cardsPaneModel,
                previousScreen = previousScreen.value,
                selectedScreen = selectedScreen.value,
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            )

            when (selectedScreen.value) {
                SelectedScreen.SPACES -> SpacesGridBottomBar(
                    isDoneEnabled = !hasNoTabs.value,
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
