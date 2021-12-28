package com.neeva.app.browsing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Patterns
import com.neeva.app.NeevaConstants.appSearchURL

fun Uri.baseDomain() : String? {
    val authority = this.authority ?: return null
    val authoritySplit = authority.split(".")
    if (authoritySplit.size < 2) return null
    return authoritySplit.takeLast(2).joinToString(".")
}

fun String.toUri() : Uri? {
    // WEB_URL doesn't match port numbers. Special case "localhost:" to aid
    // testing where a port is remapped.
    // Use WEB_URL first to ensure this matches urls such as 'https.'
    if (Patterns.WEB_URL.matcher(this).matches() || this.startsWith("http://localhost:")) {
        // WEB_URL matches relative urls (relative meaning no scheme), but this branch is only
        // interested in absolute urls. Fall through if no scheme is supplied.
        val uri = Uri.parse(this)
        if (!uri.isRelative) return uri
    }
    if (this.startsWith("www.") || this.indexOf(":") == -1) {
        val url = "http://$this"
        if (Patterns.WEB_URL.matcher(url).matches()) {
            return Uri.parse(url)
        }
    }
    return Uri.parse("").buildUpon().build()
}

fun String.toSearchUri(): Uri {
    return Uri.parse(appSearchURL)
        .buildUpon()
        .appendQueryParameter("q", this)
        .appendQueryParameter("src", "nvobar")
        .build()
}

/** Generates a single-colored Bitmap from the given Uri. */
fun Uri?.toFaviconBitmap(): Bitmap {
    val color = hashCode().or(0xff000000.toInt())
    val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)
    return bitmap
}