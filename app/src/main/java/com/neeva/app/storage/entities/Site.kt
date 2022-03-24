package com.neeva.app.storage.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Records information about a particular URL that the user has visited. */
@Entity(
    tableName = "Site",
    indices = [Index(value = ["siteURL"], unique = true)]
)
data class Site(
    @PrimaryKey(autoGenerate = true) val siteUID: Int = 0,

    val siteURL: String,

    val title: String? = null,

    @Embedded val largestFavicon: Favicon?
)
