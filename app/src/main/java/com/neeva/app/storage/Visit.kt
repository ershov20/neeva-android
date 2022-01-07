package com.neeva.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class Visit(
    @PrimaryKey(autoGenerate = true) val visitUID: Int = 0,
    val visitRootID: Long,
    val visitType: Int,
    val timestamp: Date,

    /** For passing a Visit down the stack and setting the siteUID in repository */
    val visitedSiteUID: Int = 0
) {
    enum class VisitType {
        SEARCH, LINK, TOP_SITES
    }
}
