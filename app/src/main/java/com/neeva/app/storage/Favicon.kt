package com.neeva.app.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

/** Stores information about a website's favicon.  The image is stored as an encoded Data URI. */
data class Favicon(
    val faviconURL: String?,
    val encodedImage: String?,
    val width: Int,
    val height: Int,
) {
    companion object {
        /** Returns the biggest favicon among the ones given, or null if both were null. */
        fun bestFavicon(first: Favicon?, second: Favicon?): Favicon? {
            return when {
                first == null -> second
                second == null -> first
                first.width >= second.width -> first
                else -> second
            }
        }
    }

    fun toBitmap(): Bitmap? {
        val encoded = this.encodedImage
        return if (encoded.isNullOrEmpty()) {
            null
        } else {
            val byteArray = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
    }
}

fun Bitmap.toFavicon(): Favicon {
    // Ensure that the Bitmap is a reasonable size to avoid storing a ridiculously large string.
    val maxSize = 48
    val scaledBitmap = if (width > maxSize || height > maxSize) {
        val aspectRatio = width / height.toFloat()
        if (width > height) {
            scale(maxSize, (maxSize / aspectRatio).toInt())
        } else {
            scale((maxSize * aspectRatio).toInt(), maxSize)
        }
    } else {
        this
    }

    val stream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray: ByteArray = stream.toByteArray()
    val encoded: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
    return Favicon(null, encoded, scaledBitmap.width, scaledBitmap.height)
}
