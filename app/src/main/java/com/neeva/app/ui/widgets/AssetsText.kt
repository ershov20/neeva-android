// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.neeva.app.ui.theme.Dimensions
import java.io.InputStreamReader
import timber.log.Timber

/**
 * Loads a text file from the app's assets and displays it in a horizontally scrollable container.
 */
@Composable
fun AssetsText(assetFilename: String) {
    val context = LocalContext.current
    val assetString = produceState(initialValue = "") {
        val stringBuilder = StringBuilder()
        try {
            context.assets.open(assetFilename).use {
                InputStreamReader(it).use { reader ->
                    val allLines = reader.readLines()
                    allLines.forEach { line ->
                        stringBuilder.append("$line\n")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("AssetsText").e(e, "Failed to open asset")
        }

        value = stringBuilder.toString()
    }

    Text(
        text = assetString.value,
        style = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        ),
        modifier = Modifier
            .padding(horizontal = Dimensions.PADDING_LARGE)
            .horizontalScroll(
                rememberScrollState()
            )
    )
}
