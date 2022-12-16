// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.tooling.preview.Preview

/** Creates a bitmap that looks like a checkerboard. */
fun createSingleColorBitmap(isPortrait: Boolean, color: Int): Bitmap {
    val bitmap = if (isPortrait) {
        Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
    }

    val canvas = Canvas(bitmap)
    canvas.drawColor(color)
    return bitmap
}

fun createCheckerboardBitmap(isPortrait: Boolean): Bitmap {
    val bitmap = if (isPortrait) {
        Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
    }

    val pixelSize = 64
    val canvas = Canvas(bitmap)
    val paint = Paint()
    for (left in 0 until bitmap.width step pixelSize) {
        for (top in 0 until bitmap.height step pixelSize) {
            val column = left / pixelSize
            val row = top / pixelSize
            paint.color = if ((column + row) % 2 == 0) {
                Color.DKGRAY
            } else {
                Color.LTGRAY
            }
            canvas.drawRect(
                left.toFloat(),
                top.toFloat(),
                (left + pixelSize).toFloat(),
                (top + pixelSize).toFloat(),
                paint
            )
        }
    }

    return bitmap
}

val previewCardGridTitles by lazy {
    listOf(
        "Really long title that should cause the title to ellipsize probably",
        "short",
        "Amazon.com",
        "Ad-free, private search",
        "Some other amazing site",
        "Yep, another site",
        "Drink more Ovaltine"
    )
}

@Preview(locale = "en")
@Preview(locale = "en", fontScale = 2.0f)
@Preview(locale = "fr")
@Preview(locale = "de")
@Preview(locale = "he")
annotation class PortraitPreviews

@Preview(locale = "en")
@Preview(locale = "en", fontScale = 2.0f)
annotation class NoTextPortraitPreviews

@Preview(locale = "en", uiMode = UI_MODE_NIGHT_YES)
@Preview(locale = "en", fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES)
@Preview(locale = "fr", uiMode = UI_MODE_NIGHT_YES)
@Preview(locale = "de", uiMode = UI_MODE_NIGHT_YES)
@Preview(locale = "he", uiMode = UI_MODE_NIGHT_YES)
annotation class PortraitPreviewsDark

@Preview(widthDp = 731, heightDp = 390, locale = "en")
@Preview(widthDp = 731, heightDp = 390, locale = "en", fontScale = 2.0f)
@Preview(widthDp = 731, heightDp = 390, locale = "fr")
@Preview(widthDp = 731, heightDp = 390, locale = "de")
@Preview(widthDp = 731, heightDp = 390, locale = "he")
annotation class LandscapePreviews

/*
 * Allows the height of the preview to match the height of the content,
 */
@Preview(widthDp = 731, locale = "en")
@Preview(widthDp = 731, locale = "en", fontScale = 2.0f)
@Preview(widthDp = 731, locale = "fr")
@Preview(widthDp = 731, locale = "de")
@Preview(widthDp = 731, locale = "he")
annotation class UnboundedLandscapePreviews

@Preview(widthDp = 731, heightDp = 390, locale = "en", uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 731, heightDp = 390, locale = "en", fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 731, heightDp = 390, locale = "fr", uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 731, heightDp = 390, locale = "de", uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 731, heightDp = 390, locale = "he", uiMode = UI_MODE_NIGHT_YES)
annotation class LandscapePreviewsDark
