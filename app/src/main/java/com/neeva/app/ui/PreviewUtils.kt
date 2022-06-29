package com.neeva.app.ui

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
@Preview(locale = "he")
annotation class PortraitPreviews

@Preview(widthDp = 731, heightDp = 390, locale = "en")
@Preview(widthDp = 731, heightDp = 390, locale = "en", fontScale = 2.0f)
@Preview(widthDp = 731, heightDp = 390, locale = "he")
annotation class LandscapePreviews
