package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.Entity

/** Records information about navigations that were triggered by a Search As You Type query. */
@Entity(
    tableName = "SearchNavigation",
    primaryKeys = ["tabId", "navigationEntryIndex"]
)
data class SearchNavigation(
    val tabId: String,

    /** Index of the navigation where the SAYT query was initiated. */
    val navigationEntryIndex: Int,

    /** URL of the navigation that resulted from tapping on a SAYT result. */
    val navigationEntryUri: Uri,

    /** Query that was performed. */
    val searchQuery: String
)
