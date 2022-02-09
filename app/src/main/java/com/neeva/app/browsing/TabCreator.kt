package com.neeva.app.browsing

import android.net.Uri

fun interface TabCreator {
    /**
     * Creates a new foreground tab and shows the given [uri].
     * @param uri URI to load in the tab
     * @param parentTabId If non-null: the GUID of the tab that spawned this tab
     * @param isViaIntent True if created by a VIEW Intent
     */
    fun createTabWithUri(uri: Uri, parentTabId: String?, isViaIntent: Boolean)
}
