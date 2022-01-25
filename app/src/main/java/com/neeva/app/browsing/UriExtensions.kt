package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.NeevaConstants.appSearchURL

fun String.toSearchUri(): Uri {
    return Uri.parse(appSearchURL)
        .buildUpon()
        .appendQueryParameter("q", this)
        .appendQueryParameter("src", "nvobar")
        .build()
}

fun Uri.neevaSearchQuery(): String? {
    val isNeevaSearch = this.toString().startsWith(appSearchURL)
    return if (isNeevaSearch) this.getQueryParameter("q") else null
}
