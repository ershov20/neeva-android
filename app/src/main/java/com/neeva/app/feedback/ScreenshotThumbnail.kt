// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.feedback

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.createCheckerboardBitmap
import com.neeva.app.ui.theme.Dimensions

@Composable
fun ScreenshotThumbnail(
    bitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    val it = bitmap.asImageBitmap()

    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(Dimensions.RADIUS_TINY),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(Dimensions.RADIUS_TINY)
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shadowElevation = 2.dp,
                modifier = Modifier.padding(Dimensions.PADDING_LARGE)
            ) {
                Image(
                    bitmap = it,
                    contentDescription = stringResource(
                        R.string.submit_feedback_share_screenshot_preview
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xffff00ff)
@Composable
fun ScreenshotThumbnail_LightTheme_Portrait() {
    val bitmap = createCheckerboardBitmap(isPortrait = true)
    NeevaThemePreviewContainer(useDarkTheme = false) {
        ScreenshotThumbnail(bitmap = bitmap)
    }
}

@Preview(showBackground = true, backgroundColor = 0xffff00ff)
@Composable
fun ScreenshotThumbnail_LightTheme_Landscape() {
    val bitmap = createCheckerboardBitmap(isPortrait = false)
    NeevaThemePreviewContainer(useDarkTheme = false) {
        ScreenshotThumbnail(bitmap = bitmap)
    }
}

@Preview(showBackground = true, backgroundColor = 0xffff00ff)
@Composable
fun ScreenshotThumbnail_DarkTheme_Portrait() {
    val bitmap = createCheckerboardBitmap(isPortrait = true)
    NeevaThemePreviewContainer(useDarkTheme = true) {
        ScreenshotThumbnail(bitmap = bitmap)
    }
}

@Preview(showBackground = true, backgroundColor = 0xffff00ff)
@Composable
fun ScreenshotThumbnail_DarkTheme_Landscape() {
    val bitmap = createCheckerboardBitmap(isPortrait = false)
    NeevaThemePreviewContainer(useDarkTheme = true) {
        ScreenshotThumbnail(bitmap = bitmap)
    }
}
