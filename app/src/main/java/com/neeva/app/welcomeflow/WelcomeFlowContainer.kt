// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.icons.WordMark
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@Composable
fun WelcomeFlowContainer(
    headerText: String,
    onBack: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit
) {
    // Since this composable runs in a separate activity from NeevaActivity, we don't have to worry
    // about setting the status bar color back to the original color.
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(ColorPalette.Brand.Blue, darkIcons = false)

    // TODO(kobec): try to use column with more indicator
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Necessary because when you scroll up/down to the maximums in this content,
                // you can actually see a bit of this Surface underneath.
                // To ensure that the background Surface matches its content color, we used this
                // hack.
                brush = Brush.verticalGradient(
                    Pair(0.05f, ColorPalette.Brand.Blue),
                    Pair(0.05f, MaterialTheme.colorScheme.background)
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            WelcomeFlowHeader(
                headerText = headerText,
                onBack = onBack,
            )
            Box(
                Modifier.background(
                    // Necessary because the bottom of this box actually shows up as a small line of
                    // pixels.
                    brush = Brush.verticalGradient(
                        Pair(0.5f, ColorPalette.Brand.Blue),
                        Pair(0.5f, MaterialTheme.colorScheme.background)
                    )
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = Dimensions.RADIUS_HUGE,
                        topEnd = Dimensions.RADIUS_HUGE
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    content(
                        Modifier.padding(
                            horizontal = dimensionResource(id = R.dimen.welcome_flow_padding)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeFlowHeader(
    headerText: String,
    onBack: (() -> Unit)? = null
) {
    Surface(color = ColorPalette.Brand.Blue, contentColor = Color.White) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Spacer(Modifier.height(Dimensions.PADDING_LARGE))
                WordMark(
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                    modifier = Modifier.width(89.dp)
                )
                Spacer(Modifier.height(Dimensions.PADDING_LARGE))
                Text(
                    text = headerText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
                Spacer(Modifier.height(Dimensions.PADDING_HUGE))
            }
            if (onBack != null) {
                RowActionIconButton(
                    onTapAction = onBack,
                    contentDescription = stringResource(R.string.toolbar_go_back),
                    actionType = RowActionIconParams.ActionType.BACK,
                    color = LocalContentColor.current,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}
