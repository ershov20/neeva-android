// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TabData")
data class TabData(
    @PrimaryKey
    val id: String,

    /** URL of the webpage that was last displayed. */
    val url: Uri?,

    /** Title of the webpage that was last displayed. */
    val title: String?,

    /** When the tab was last active.  Typically set by System.currentTimeMillis(). */
    val lastActiveMs: Long,

    /** Whether or not the tab is currently archived. */
    val isArchived: Boolean
)
