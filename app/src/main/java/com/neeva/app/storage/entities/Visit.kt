package com.neeva.app.storage.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "Visit"
)
data class Visit(
    @PrimaryKey(autoGenerate = true) val visitUID: Int = 0,

    val timestamp: Date,

    /** Tracks the ID of the [Site] that was visited. */
    val visitedSiteUID: Int = 0,

    /** Indicates whether or not the Visit should be deleted during the next purge. */
    @ColumnInfo(defaultValue = "0")
    val isMarkedForDeletion: Boolean = false
)
