package com.neeva.app.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Records information about a particular host that the user has visited. */
@Entity(tableName = "HostInfo")
data class HostInfo(
    @PrimaryKey
    val host: String,

    val isTrackingAllowed: Boolean
)
