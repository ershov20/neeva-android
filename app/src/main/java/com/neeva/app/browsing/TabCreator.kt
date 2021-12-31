package com.neeva.app.browsing

import android.net.Uri

fun interface TabCreator {
    /** Creates a new foreground tab and shows the given [uri]. */
    fun createTabWithUri(uri: Uri, parentTabId: String?)
}
