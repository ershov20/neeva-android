package com.neeva.app.spaces

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.neeva.app.NeevaConstants.appSpacesURL
import com.neeva.app.type.SpaceACLLevel

data class Space(
    val id: String,
    val name: String,
    val lastModifiedTs: String,
    val thumbnail: String?,
    val resultCount: Int,
    val isDefaultSpace: Boolean,
    val isShared: Boolean,
    val isPublic: Boolean,
    val userACL: SpaceACLLevel,
) {
    companion object {
        const val DATA_URI_PREFIX = "data:image/jpeg;base64,"
    }

    val url: Uri = Uri.parse("$appSpacesURL/$id")

    var contentURLs: Set<Uri>? = null
    var contentData: List<SpaceEntityData>? = null

    fun thumbnailAsBitmap(): Bitmap? {
        val encoded = thumbnail
            ?.takeIf { it.startsWith(DATA_URI_PREFIX) }
            ?.drop(DATA_URI_PREFIX.length)
            ?: return null

        return try {
            val byteArray = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
