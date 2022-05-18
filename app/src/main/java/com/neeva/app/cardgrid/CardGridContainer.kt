package com.neeva.app.cardgrid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.spaces.SpaceGrid
import com.neeva.app.cardgrid.tabs.TabGrid
import com.neeva.app.ui.AnimationConstants

@Composable
fun CardGridContainer(
    webLayerModel: WebLayerModel,
    cardsPaneModel: CardsPaneModel,
    previousScreen: SelectedScreen?,
    selectedScreen: SelectedScreen,
    neevaConstants: NeevaConstants,
    modifier: Modifier = Modifier
) {
    val browsersFlow = webLayerModel.browsersFlow.collectAsState()

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = selectedScreen == SelectedScreen.INCOGNITO_TABS,
            enter = slideInHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                initialOffsetX = { fullWidth -> -fullWidth }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                targetOffsetX = { fullWidth -> -fullWidth }
            )
        ) {
            browsersFlow.value.incognitoBrowserWrapper?.let {
                TabGrid(
                    browserWrapper = it,
                    cardsPaneModel = cardsPaneModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(
            visible = selectedScreen == SelectedScreen.REGULAR_TABS,
            enter = slideInHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                initialOffsetX = { fullWidth ->
                    if (previousScreen == SelectedScreen.INCOGNITO_TABS) {
                        fullWidth
                    } else {
                        -fullWidth
                    }
                }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                targetOffsetX = { fullWidth ->
                    if (selectedScreen == SelectedScreen.INCOGNITO_TABS) {
                        fullWidth
                    } else {
                        -fullWidth
                    }
                }
            )
        ) {
            TabGrid(
                browserWrapper = browsersFlow.value.regularBrowserWrapper,
                cardsPaneModel = cardsPaneModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = selectedScreen == SelectedScreen.SPACES,
            enter = slideInHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                initialOffsetX = { fullWidth -> fullWidth }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                targetOffsetX = { fullWidth -> fullWidth }
            )
        ) {
            SpaceGrid(
                browserWrapper = browsersFlow.value.regularBrowserWrapper,
                cardsPaneModel = cardsPaneModel,
                neevaConstants = neevaConstants,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
