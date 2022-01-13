package com.neeva.app.browsing

import android.net.Uri

/** Information required to render a Tab in the UI. */
data class TabInfo(
    val id: String,
    val parentTabId: String? = null,
    val url: Uri?,
    val title: String?,
    val isSelected: Boolean
)
