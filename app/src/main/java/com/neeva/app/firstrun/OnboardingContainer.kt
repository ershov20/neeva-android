// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.OnboardingStickyFooter
import com.neeva.app.firstrun.widgets.buttons.CloseButton
import com.neeva.app.firstrun.widgets.textfields.rememberIsKeyboardOpen
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.widgets.ColumnWithMoreIndicator

@Composable
fun OnboardingContainer(
    showBrowser: () -> Unit,
    useSignUpStickyFooter: Boolean,
    stickyFooterOnClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val backgroundColor = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.background
    } else {
        ColorPalette.Brand.Offwhite
    }

    Surface(color = backgroundColor) {
        Box(Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            val isKeyboardOpen by rememberIsKeyboardOpen()
            Column {
                ColumnWithMoreIndicator(
                    scrollState = scrollState,
                    color = backgroundColor,
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    content(
                        Modifier
                            .fillMaxHeight()
                            .padding(
                                horizontal = dimensionResource(id = R.dimen.welcome_flow_padding)
                            )
                    )
                }

                if (!isKeyboardOpen) {
                    OnboardingStickyFooter(useSignUpStickyFooter, scrollState, stickyFooterOnClick)
                }
            }

            CloseButton(
                onClick = showBrowser,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
