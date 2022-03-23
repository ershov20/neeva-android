package com.neeva.app.ui.widgets.overlay

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OverlaySheet(
    config: OverlaySheetConfig = OverlaySheetConfig.default,
    content: @Composable () -> Unit
) {
    val appNavModel = LocalAppNavModel.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Spacer(
            modifier = Modifier
                .clickable { appNavModel.showBrowser() }
                .fillMaxWidth()
                .then(
                    when (config.height) {
                        OverlaySheetHeightConfig.HALF_SCREEN -> Modifier.fillMaxHeight(0.5f)
                        OverlaySheetHeightConfig.WRAP_CONTENT -> Modifier.weight(1f)
                    }
                )
                .background(Color.Transparent)
        )

        Box(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .fillMaxWidth()
                .then(
                    when (config.height) {
                        OverlaySheetHeightConfig.HALF_SCREEN -> Modifier.fillMaxHeight()
                        OverlaySheetHeightConfig.WRAP_CONTENT -> Modifier.wrapContentHeight()
                    }
                )
                .pointerInput(Unit) {}
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
