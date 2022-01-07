package com.neeva.app.storage

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(indices = [Index(value = ["siteURL"], unique = true)])
data class Site(
    @PrimaryKey(autoGenerate = true) val siteUID: Int = 0,
    val siteURL: String,
    val visitCount: Int = 1,
    val lastVisitTimestamp: Date,
    @Embedded val metadata: SiteMetadata?,
    @Embedded val largestFavicon: Favicon?
) {
    // TODO(dan.alcantara): We should be limiting how large these strings can get -- especially if they
    //                      are provided by the website itself.
    data class SiteMetadata(
        val imageURL: String? = null,
        val title: String? = null,
        val description: String? = null,
        val entityType: Int = 0,
    )

    enum class EntityType {
        UNKNOWN, ARTICLE, PRODUCT
    }
}
