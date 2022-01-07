package com.neeva.app.storage

import androidx.room.Embedded
import androidx.room.Relation

data class SiteWithVisits(
    @Embedded val site: Site,
    @Relation(
        parentColumn = "siteUID",
        entityColumn = "visitedSiteUID"
    )
    val visits: List<Visit>
)
