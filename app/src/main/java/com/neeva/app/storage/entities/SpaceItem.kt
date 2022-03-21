package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SpaceItem")
data class SpaceItem(
    @PrimaryKey val id: String,
    val spaceID: String,
    val url: Uri?,
    val title: String?,
    val snippet: String?,
    val thumbnail: Uri?,
)
