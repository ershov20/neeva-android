package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.neeva.app.NeevaConstants
import com.neeva.app.type.SpaceACLLevel

@androidx.room.Entity(tableName = "Space")
data class Space(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(defaultValue = "")
    val description: String = "",
    val lastModifiedTs: String,
    var thumbnail: Uri?,
    val resultCount: Int,
    val isDefaultSpace: Boolean,
    val isShared: Boolean,
    val isPublic: Boolean,
    val userACL: SpaceACLLevel,

    @ColumnInfo(defaultValue = "")
    val ownerName: String = "",

    @ColumnInfo(defaultValue = "")
    val ownerPictureURL: Uri? = null,

    @ColumnInfo(defaultValue = "0")
    val numViews: Int = 0,

    @ColumnInfo(defaultValue = "0")
    val numFollowers: Int = 0
) {
    fun url(neevaConstants: NeevaConstants): Uri =
        Uri.parse("${neevaConstants.appSpacesURL}/$id")
}
