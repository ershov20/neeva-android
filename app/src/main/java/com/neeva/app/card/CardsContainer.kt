package com.neeva.app.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.neeva.app.LocalAppNavModel
import com.neeva.app.browsing.WebLayerModel

enum class SelectedScreen {
    INCOGNITO_TABS, REGULAR_TABS, SPACES
}

@Composable
fun CardsContainer(webLayerModel: WebLayerModel) {
    val appNavModel = LocalAppNavModel.current
    val currentBrowser by webLayerModel.browserWrapperFlow.collectAsState()
    val isCurrentlyIncognito = currentBrowser.isIncognito

    val cardGridModel = remember(webLayerModel, currentBrowser, appNavModel) {
        CardGridModelImpl(webLayerModel, currentBrowser, appNavModel)
    }

    val selectedScreen = if (isCurrentlyIncognito) {
        SelectedScreen.INCOGNITO_TABS
    } else {
        SelectedScreen.REGULAR_TABS
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModeSwitcher(
                selectedScreen = selectedScreen,
                onSwitchScreen = cardGridModel::switchScreen
            )

            CardGrid(
                browserWrapper = currentBrowser,
                selectedScreen = selectedScreen,
                cardGridModel = cardGridModel,
                modifier = Modifier.fillMaxWidth().weight(1.0f)
            )

            TabGridBottomBar(
                currentBrowser = currentBrowser,
                cardGridModel = cardGridModel
            )
        }
    }
}
