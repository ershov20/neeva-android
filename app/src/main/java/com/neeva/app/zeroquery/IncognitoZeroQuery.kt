// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.AnnotatedSpannable

@Composable
fun IncognitoZeroQuery(topContent: @Composable () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "IncognitoZeroQuery" }
    ) {
        topContent()

        IncognitoZeroQueryDisclaimer(
            modifier = Modifier.weight(1.0f)
        )
    }
}

@Composable
fun IncognitoZeroQueryDisclaimer(
    modifier: Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.inverseSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimensions.PADDING_LARGE)
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(Dimensions.RADIUS_SMALL)
        ) {
            val spacingDp = with(LocalDensity.current) {
                MaterialTheme.typography.bodyMedium.lineHeight.toDp()
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Dimensions.PADDING_LARGE)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_incognito),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(backgroundColor),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = LocalContentColor.current, shape = CircleShape)
                        .padding(Dimensions.PADDING_SMALL)
                )

                Spacer(modifier = Modifier.height(spacingDp))

                Text(
                    text = stringResource(id = R.string.incognito_zero_query_title),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(spacingDp))

                AnnotatedSpannable(
                    rawHtml = stringResource(id = R.string.incognito_zero_query_body),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current
                    )
                )

                Spacer(modifier = Modifier.height(spacingDp))

                AnnotatedSpannable(
                    rawHtml = stringResource(id = R.string.incognito_zero_query_footer),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = LocalContentColor.current
                    )
                )
            }
        }
    }
}

@Preview(fontScale = 1.0f, locale = "en")
@Preview(fontScale = 2.0f, locale = "en")
@Composable
fun IncognitoZeroQueryPreview_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        IncognitoZeroQuery()
    }
}

@Preview(fontScale = 1.0f, locale = "en")
@Composable
fun IncognitoZeroQueryPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        IncognitoZeroQuery()
    }
}
