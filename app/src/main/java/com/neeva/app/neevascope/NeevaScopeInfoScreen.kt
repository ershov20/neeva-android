// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.buttons.CloseButton
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun NeevaScopeInfoScreen(
    @StringRes buttonTextId: Int,
    tapButton: () -> Unit,
    dismissSheet: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            CloseButton(
                onClick = dismissSheet,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
            if (constraints.maxWidth > constraints.maxHeight) {
                // Landscape
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(1.0f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cheatsheet),
                            contentDescription = null,
                            modifier = Modifier.height(224.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .padding(horizontal = Dimensions.PADDING_LARGE)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NeevaScopeInfoHeader()

                        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

                        NeevaScopeInfoBody()

                        Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

                        NeevaScopeInfoButton(
                            buttonTextId = buttonTextId,
                            onClick = tapButton,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            } else {
                // Portrait
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = Dimensions.PADDING_HUGE)
                        .verticalScroll(rememberScrollState())
                ) {
                    NeevaScopeInfoHeader()

                    Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

                    NeevaScopeInfoBody()

                    Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

                    Image(
                        painter = painterResource(id = R.drawable.cheatsheet),
                        contentDescription = null,
                        modifier = Modifier
                            .height(224.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    NeevaScopeInfoButton(
                        buttonTextId = buttonTextId,
                        onClick = tapButton,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun NeevaScopeInfoHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_neeva_logo),
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))

        Text(
            text = stringResource(id = R.string.neevascope),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun NeevaScopeInfoBody() {
    Column {
        Text(
            text = stringResource(id = R.string.neevascope_intro_title),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
        Text(
            text = stringResource(id = R.string.neevascope_intro_body),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun NeevaScopeInfoButton(
    @StringRes buttonTextId: Int,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
            .height(48.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = buttonTextId),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun NeevaScopeInfo_Light_Preview() {
    NeevaTheme {
        NeevaScopeInfoScreen(
            buttonTextId = R.string.neevascope_got_it,
            tapButton = {},
            dismissSheet = {}
        )
    }
}

@Preview("Dark 1x scale", locale = "en", uiMode = UI_MODE_NIGHT_YES)
@Preview("Dark 2x scale", locale = "en", fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES)
@Preview(
    "Pixel 2 landscape, 1x scale", widthDp = 731, heightDp = 390, locale = "en",
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun NeevaScopeInfo_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        NeevaScopeInfoScreen(
            buttonTextId = R.string.neevascope_got_it,
            tapButton = {},
            dismissSheet = {}
        )
    }
}
