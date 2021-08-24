package com.neeva.app.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OverlaySheet(
    appNavModel: AppNavModel,
    visibleState: AppNavState,
    config: OverlaySheetHeightConfig = OverlaySheetHeightConfig.HALF_SCREEN,
    content: @Composable () -> Unit,
) {
    val state: AppNavState by appNavModel.state.observeAsState(AppNavState.HIDDEN)
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = state == visibleState,
        enter = slideInVertically(
            initialOffsetY = { with(density) { 400.dp.roundToPx() } }
        ),
        exit = slideOutVertically(
            targetOffsetY = { with(density) { 400.dp.roundToPx() } }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Spacer(modifier = Modifier
                .clickable { appNavModel.setContentState(AppNavState.HIDDEN) }
                .fillMaxWidth()
                .then(
                    when (config) {
                        OverlaySheetHeightConfig.HALF_SCREEN -> Modifier.fillMaxHeight(0.5f)
                        OverlaySheetHeightConfig.WRAP_CONTENT -> Modifier.weight(1f)
                    }
                )
                .background(Color.Transparent))

            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .shadow(4.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .fillMaxWidth()
                    .then(
                        when (config) {
                            OverlaySheetHeightConfig.HALF_SCREEN -> Modifier.fillMaxHeight()
                            OverlaySheetHeightConfig.WRAP_CONTENT -> Modifier.wrapContentHeight()
                        }
                    )
                    .background(MaterialTheme.colors.background)
            ) {
                content()
            }
        }
    }
}

enum class OverlaySheetHeightConfig {
    HALF_SCREEN, WRAP_CONTENT
}
