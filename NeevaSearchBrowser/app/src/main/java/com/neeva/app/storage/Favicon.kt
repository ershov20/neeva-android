package com.neeva.app.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.neeva.app.NeevaBrowser
import com.neeva.app.R
import java.io.ByteArrayOutputStream

data class Favicon(
    val faviconURL: String?,
    val encodedImage: String?,
    val width: Int,
    val height: Int,
) {
    companion object {
        val defaultFavicon: Bitmap by lazy {
            BitmapFactory.decodeResource(NeevaBrowser.context.resources, R.drawable.globe)
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

    infix fun larger(given: Favicon?) : Favicon {
        return when {
            given == null -> this
            given.width > this.width -> given
            else -> this
        }
    }
}

fun Bitmap.toFavicon(): Favicon {
    val baos = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, baos)
    val byteArray: ByteArray = baos.toByteArray()
    val encoded: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
    return Favicon(null, encoded, width, height)
}
