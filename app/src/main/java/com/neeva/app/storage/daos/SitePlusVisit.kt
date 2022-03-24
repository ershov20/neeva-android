package com.neeva.app.storage.daos

import androidx.room.Embedded
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit

data class SitePlusVisit(
    @Embedded
    val site: Site,

    @Embedded
    val visit: Visit
)
