package com.neeva.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.neeva.app.BrowserUI
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.TabToolbar
import com.neeva.app.TabToolbarModel
import com.neeva.app.browsing.WebLayerModel
import kotlinx.coroutines.flow.StateFlow

// TODO(dan.alcantara): Investigate using Compose Scaffold.
@Composable
fun BrowserScaffold(
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    webLayerModel: WebLayerModel
) {
    val browserWrapper by webLayerModel.browserWrapperFlow.collectAsState()
    val appNavModel = LocalAppNavModel.current

    CompositionLocalProvider(LocalBrowserWrapper provides browserWrapper) {
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
                urlBarModel = browserWrapper.urlBarModel,
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
    }
}
