package com.neeva.app.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.neeva.app.LocalAppNavModel
import com.neeva.app.browsing.WebLayerModel

enum class SelectedScreen {
    INCOGNITO_TABS, REGULAR_TABS
}

@Composable
fun CardsContainer(webLayerModel: WebLayerModel) {
    val appNavModel = LocalAppNavModel.current

    val cardGridModel = remember(webLayerModel, appNavModel) {
        CardGridModelImpl(webLayerModel, appNavModel)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModeSwitcher(
                webLayerModel = webLayerModel,
                onSwitchScreen = cardGridModel::switchScreen
            )

            CardGridContainer(
                webLayerModel = webLayerModel,
                cardGridModel = cardGridModel,
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            )

            TabGridBottomBar(
                webLayerModel = webLayerModel,
                cardGridModel = cardGridModel
            )
        }
    }
}
