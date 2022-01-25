package com.neeva.app.storage.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/** Records information about a particular URL that the user has visited. */
@Entity(
    tableName = "Site",
    indices = [Index(value = ["siteURL"], unique = true)]
)
data class Site(
    @PrimaryKey(autoGenerate = true) val siteUID: Int = 0,
    val siteURL: String,

    /** Deprecated. */
    val visitCount: Int = 1,

    /** Deprecated. */
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
