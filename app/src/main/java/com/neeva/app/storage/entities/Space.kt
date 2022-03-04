package com.neeva.app.storage.entities

import android.net.Uri
import com.neeva.app.NeevaConstants.appSpacesURL
import com.neeva.app.type.SpaceACLLevel

data class Space(
    val id: String,
    val name: String,
    val lastModifiedTs: String,
    var thumbnail: Uri?,
    val resultCount: Int,
    val isDefaultSpace: Boolean,
    val isShared: Boolean,
    val isPublic: Boolean,
    val userACL: SpaceACLLevel,
) {
    val url: Uri = Uri.parse("$appSpacesURL/$id")
    var contentURLs: Set<Uri>? = null
    var contentData: List<SpaceEntityData>? = null
}
