package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.NeevaConstants

fun String.toSearchUri(): Uri {
    return Uri.parse(NeevaConstants.appSearchURL)
        .buildUpon()
        .appendQueryParameter("q", this)
        .appendQueryParameter("src", "nvobar")
        .build()
}

fun Uri.isNeevaUri(): Boolean {
    return authority == NeevaConstants.appHost
}

fun Uri.isNeevaSearchUri(): Boolean {
    return toString().startsWith(NeevaConstants.appSearchURL)
}

fun Uri.neevaSearchQuery(): String? {
    val isNeevaSearch = this.toString().startsWith(NeevaConstants.appSearchURL)
    return if (isNeevaSearch) this.getQueryParameter("q") else null
}
