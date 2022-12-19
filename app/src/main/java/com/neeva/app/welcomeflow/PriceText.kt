// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun PriceText(
    price: String,
    formattedText: String,
    modifier: Modifier = Modifier
) {
    val baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val annotatedString = buildAnnotatedString {
        withStyle(baseTextStyle.toSpanStyle()) {
            append(formattedText)
            val priceStartIndex = formattedText.indexOf(price)
            val priceEndIndex = priceStartIndex + price.length
            val priceStyle = MaterialTheme.typography.titleMedium.toSpanStyle()

            addStyle(priceStyle, priceStartIndex, priceEndIndex)
        }
    }

    BasicText(
        text = annotatedString,
        style = baseTextStyle,
        modifier = modifier
    )
}
