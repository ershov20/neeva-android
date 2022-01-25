package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.settings.SettingsModel
import com.neeva.app.ui.theme.NeevaTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LocalEnvironmentState(
    val browserWrapper: BrowserWrapper,
    val appNavModel: AppNavModel,
    val settingsModel: SettingsModel,
    val historyManager: HistoryManager
)
val LocalEnvironment = compositionLocalOf<LocalEnvironmentState> { error("No value set") }

@Composable
fun ActivityUI(
    browserWrapperFlow: StateFlow<BrowserWrapper>,
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    appNavModel: AppNavModel,
    webLayerModel: WebLayerModel,
    settingsModel: SettingsModel,
    apolloClient: ApolloClient,
    historyManager: HistoryManager
) {
    val coroutineScope = rememberCoroutineScope()
    val browserWrapper by browserWrapperFlow.collectAsState()

    val environment = LocalEnvironmentState(
        browserWrapper = browserWrapper,
        appNavModel = appNavModel,
        settingsModel = settingsModel,
        historyManager = historyManager
    )
    CompositionLocalProvider(LocalEnvironment provides environment) {
        NeevaTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                // Bottom controls: Back, forward, app menu, ...
                val bottomOffset by bottomControlOffset.collectAsState()
                val bottomOffsetDp = with(LocalDensity.current) { bottomOffset.toDp() }
                TabToolbar(
                    model = TabToolbarModel(
                        onNeevaMenu = appNavModel::showNeevaMenu,
                        onAddToSpace = appNavModel::showAddToSpace,
                        onTabSwitcher = {
                            browserWrapper.takeScreenshotOfActiveTab {
                                appNavModel.showCardGrid()
                            }
                        },
                        goBack = browserWrapper.activeTabModel::goBack,
                        goForward = browserWrapper.activeTabModel::goForward,
                    ),
                    activeTabModel = browserWrapper.activeTabModel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = bottomOffsetDp)
                )

                // Top controls: URL bar, Suggestions, Zero Query, ...
                // Placed after the bottom controls so that it is drawn over the bottom controls
                // when necessary.
                val topOffset by topControlOffset.collectAsState()
                val topOffsetDp = with(LocalDensity.current) { topOffset.toDp() }
                BrowserUI(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = topOffsetDp)
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            // All the other screens in the app.
            AppNav(
                webLayerModel = webLayerModel,
                modifier = Modifier.fillMaxSize()
            ) { space ->
                coroutineScope.launch {
                    browserWrapper.activeTabModel.modifySpace(space, apolloClient)
                    appNavModel.showBrowser()
                }
            }
        }
    }
}
