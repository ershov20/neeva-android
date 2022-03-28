package com.neeva.app.storage.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri

/** Stores information about a website's favicon. */
data class Favicon(
    val faviconURL: String?,
    val width: Int,
    val height: Int,
) {
    companion object {
        private const val DEFAULT_SIZE = 128

        /** Generates a single-colored Bitmap from the given Uri. */
        fun Uri?.toBitmap(): Bitmap {
            val backgroundColorHashCode = hashCode().or(0xff000000.toInt())
            val text: String = this?.authority?.takeIf { it.length > 1 }?.take(1) ?: ""
            return toFaviconBitmap(text, 0.75f, backgroundColorHashCode)
        }

        fun String.toBitmap(textRatio: Float, backgroundColorHashCode: Int): Bitmap {
            val firstElement = (this.firstOrNull() ?: "").toString()
            return toFaviconBitmap(firstElement, textRatio, backgroundColorHashCode)
        }

        private fun toFaviconBitmap(
            text: String,
            textRatio: Float,
            backgroundColorHashCode: Int
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(DEFAULT_SIZE, DEFAULT_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(backgroundColorHashCode)

            val textPaint = Paint()
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE
            textPaint.textSize = DEFAULT_SIZE * textRatio

            val xPos = canvas.width / 2.0f
            val yPos = (canvas.height - textPaint.descent() - textPaint.ascent()) / 2.0f
            canvas.drawText(text.uppercase(), xPos, yPos, textPaint)

            return bitmap
        }
    }
}
