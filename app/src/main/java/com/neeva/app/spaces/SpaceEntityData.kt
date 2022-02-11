package com.neeva.app.spaces

import android.net.Uri

data class SpaceEntityData(
    val url: Uri,
    val title: String?,
    val snippet: String?,
    val thumbnail: String?,
)
