package com.neeva.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Creates a bitmap that looks like a checkerboard. */
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
