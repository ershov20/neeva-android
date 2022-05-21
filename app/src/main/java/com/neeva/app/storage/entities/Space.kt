package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neeva.app.NeevaConstants
import com.neeva.app.type.SpaceACLLevel

@Entity(tableName = "Space")
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

    val ownerName: String = "",
    val ownerPictureURL: Uri? = null,
    val numViews: Int = 0,
    val numFollowers: Int = 0
) {
    fun url(neevaConstants: NeevaConstants): Uri =
        Uri.parse("${neevaConstants.appSpacesURL}/$id")
}
