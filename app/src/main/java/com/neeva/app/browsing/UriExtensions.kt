package com.neeva.app.browsing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import com.neeva.app.NeevaConstants.appSearchURL
import com.neeva.app.storage.Favicon
import com.neeva.app.storage.toFavicon

fun String.toSearchUri(): Uri {
    return Uri.parse(appSearchURL)
        .buildUpon()
        .appendQueryParameter("q", this)
        .appendQueryParameter("src", "nvobar")
        .build()
}

/** Generates a single-colored Bitmap from the given Uri. */
fun Uri?.toFavicon(): Favicon {
    val color = hashCode().or(0xff000000.toInt())
    val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)
    return bitmap.toFavicon()
}
