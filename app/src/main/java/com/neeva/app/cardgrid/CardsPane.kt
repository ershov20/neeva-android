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
import com.neeva.app.LocalEnvironment
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.spaces.SpacesGridBottomBar
import com.neeva.app.cardgrid.tabs.TabGridBottomBar
import com.neeva.app.settings.SettingsToggle

enum class SelectedScreen {
    INCOGNITO_TABS, REGULAR_TABS, SPACES
}

@Composable
fun CardsPane(webLayerModel: WebLayerModel) {
    val cardsPaneModel = LocalCardsPaneModel.current

    val closeIncognitoTabsOnScreenSwitch = LocalEnvironment.current.settingsDataModel
        .getSettingsToggleValue(SettingsToggle.CLOSE_INCOGNITO_TABS)

    val currentBrowser by webLayerModel.currentBrowserFlow.collectAsState()
    val hasNoTabs = currentBrowser.hasNoTabsFlow().collectAsState(false)

    val requireConfirmationWhenCloseAllTabs = LocalEnvironment.current.settingsDataModel
        .getSettingsToggleValue(SettingsToggle.REQUIRE_CONFIRMATION_ON_TAB_CLOSE)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SegmentedPicker(
                selectedScreen = cardsPaneModel.selectedScreen,
                onSwitchScreen = cardsPaneModel::switchScreen,
                onLeaveIncognito = {
                    if (closeIncognitoTabsOnScreenSwitch) {
                        cardsPaneModel.closeAllTabs(currentBrowser)
                    }
                }
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
                    onCreateSpace = cardsPaneModel::createSpace,
                    onNavigateToSpacesWebsite = cardsPaneModel::showSpacesWebsite,
                    onDone = cardsPaneModel::showBrowser
                )

                else -> TabGridBottomBar(
                    isIncognito = currentBrowser.isIncognito,
                    hasNoTabs = hasNoTabs.value,
                    requireConfirmationWhenCloseAllTabs = requireConfirmationWhenCloseAllTabs,
                    onCloseAllTabs = { cardsPaneModel.closeAllTabs(currentBrowser) },
                    onOpenLazyTab = { cardsPaneModel.openLazyTab(currentBrowser) },
                    onDone = cardsPaneModel::showBrowser
                )
            }
        }
    }
}
