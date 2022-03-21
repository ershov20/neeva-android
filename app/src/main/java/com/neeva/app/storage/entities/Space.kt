package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.PrimaryKey
import com.neeva.app.NeevaConstants.appSpacesURL
import com.neeva.app.type.SpaceACLLevel

@androidx.room.Entity(tableName = "Space")
data class Space(
    @PrimaryKey val id: String,
    val name: String,
    val lastModifiedTs: String,
    var thumbnail: Uri?,
    val resultCount: Int,
    val isDefaultSpace: Boolean,
    val isShared: Boolean,
    val isPublic: Boolean,
    val userACL: SpaceACLLevel,
) {
    fun url(): Uri = Uri.parse("$appSpacesURL/$id")
}
