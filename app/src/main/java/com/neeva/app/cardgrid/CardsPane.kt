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

enum class SelectedScreen {
    INCOGNITO_TABS, REGULAR_TABS, SPACES
}

@Composable
fun CardsPane(webLayerModel: WebLayerModel) {
    val cardsPaneModel = LocalCardsPaneModel.current
    val currentBrowsers by webLayerModel.browsersFlow.collectAsState()
    val currentBrowser = currentBrowsers.getCurrentBrowser()

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CardsPaneToolbar(currentBrowser)

            CardGridContainer(
                webLayerModel = webLayerModel,
                cardsPaneModel = cardsPaneModel,
                previousScreen = cardsPaneModel.previousScreen.value,
                selectedScreen = cardsPaneModel.selectedScreen.value,
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            )
        }
    }
}
