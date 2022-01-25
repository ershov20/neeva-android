package com.neeva.app.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "Visit"
)
data class Visit(
    @PrimaryKey(autoGenerate = true) val visitUID: Int = 0,

    /** Deprecated. */
    val visitRootID: Long,

    val visitType: Int,
    val timestamp: Date,

    /** Tracks the ID of the [Site] that was visited. */
    val visitedSiteUID: Int = 0
) {
    enum class VisitType {
        SEARCH, LINK, TOP_SITES
    }
}
