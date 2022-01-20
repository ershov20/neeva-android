package com.neeva.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.settings.SettingsModel
import com.neeva.app.ui.theme.NeevaTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LocalEnvironmentState(
    val browserWrapper: BrowserWrapper,
    val appNavModel: AppNavModel
)
val LocalEnvironment = compositionLocalOf<LocalEnvironmentState> { error("No value set") }

@Composable
fun ActivityUI(
    browserWrapper: BrowserWrapper,
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    appNavModel: AppNavModel,
    webLayerModel: WebLayerModel,
    settingsModel: SettingsModel,
    apolloClient: ApolloClient
) {
    val coroutineScope = rememberCoroutineScope()

    val environment = LocalEnvironmentState(
        browserWrapper = browserWrapper,
        appNavModel = appNavModel
    )
    CompositionLocalProvider(LocalEnvironment provides environment) {
        NeevaTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top controls: URL bar, Suggestions, Zero Query, ...
                val topOffset by topControlOffset.collectAsState()
                val topOffsetDp = with(LocalDensity.current) { topOffset.toDp() }
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = topOffsetDp)
                ) {
                    BrowserUI()
                }

                // Bottom controls: Back, forward, app menu, ...
                val bottomOffset by bottomControlOffset.collectAsState()
                val bottomOffsetDp = with(LocalDensity.current) { bottomOffset.toDp() }
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = bottomOffsetDp)
                ) {
                    val isEditing by browserWrapper.urlBarModel.isEditing.collectAsState(false)
                    if (!isEditing) {
                        TabToolbar(
                            model = TabToolbarModel(
                                appNavModel::showNeevaMenu,
                                appNavModel::showAddToSpace
                            ) {
                                browserWrapper.takeScreenshotOfActiveTab {
                                    appNavModel.showCardGrid()
                                }
                            },
                            activeTabModel = browserWrapper.activeTabModel
                        )
                    }
                }
            }

            // All the other screens in the app.
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(color = Color.Transparent) {
                    AppNav(
                        webLayerModel = webLayerModel,
                        settingsModel = settingsModel
                    ) { space ->
                        coroutineScope.launch {
                            browserWrapper.activeTabModel.modifySpace(space, apolloClient)
                            appNavModel.showBrowser()
                        }
                    }
                }
            }
        }
    }
}
