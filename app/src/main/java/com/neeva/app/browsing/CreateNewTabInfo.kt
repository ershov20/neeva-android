package com.neeva.app.browsing

import android.net.Uri

data class CreateNewTabInfo(
    val uri: Uri,
    val parentTabId: String?,
    val tabOpenType: TabInfo.TabOpenType
)
