package com.neeva.app.storage.entities

import android.net.Uri

data class SpaceEntityData(
    val id: String,
    val url: Uri?,
    val title: String?,
    val snippet: String?,
    val thumbnail: Uri?,
)
