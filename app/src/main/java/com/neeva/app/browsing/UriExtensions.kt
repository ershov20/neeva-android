package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.NeevaConstants

fun String.toSearchUri(neevaConstants: NeevaConstants): Uri {
    return Uri.parse(neevaConstants.appSearchURL)
        .buildUpon()
        .appendQueryParameter("q", this)
        .appendQueryParameter("src", "nvobar")
        .build()
}

fun Uri.isNeevaUri(neevaConstants: NeevaConstants): Boolean {
    return authority == neevaConstants.appHost
}

fun Uri.isNeevaSearchUri(neevaConstants: NeevaConstants): Boolean {
    return toString().startsWith(neevaConstants.appSearchURL)
}

fun Uri.neevaSearchQuery(neevaConstants: NeevaConstants): String? {
    val isNeevaSearch = this.toString().startsWith(neevaConstants.appSearchURL)
    return if (isNeevaSearch) this.getQueryParameter("q") else null
}
