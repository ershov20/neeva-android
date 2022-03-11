package com.neeva.app.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.ui.AnimationConstants

@Composable
fun CardGridContainer(
    webLayerModel: WebLayerModel,
    cardGridModel: CardGridModel,
    modifier: Modifier = Modifier
) {
    val browsersFlow = webLayerModel.browsersFlow.collectAsState()
    val isCurrentlyIncognito = browsersFlow.value.isCurrentlyIncognito

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = isCurrentlyIncognito,
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
                CardGrid(
                    browserWrapper = it,
                    cardGridModel = cardGridModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(
            visible = !isCurrentlyIncognito,
            enter = slideInHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                initialOffsetX = { fullWidth -> fullWidth }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
                targetOffsetX = { fullWidth -> fullWidth }
            )
        ) {
            CardGrid(
                browserWrapper = browsersFlow.value.regularBrowser,
                cardGridModel = cardGridModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
