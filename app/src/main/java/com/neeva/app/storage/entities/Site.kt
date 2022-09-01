// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Records information about a particular URL that the user has visited. */
@Entity(
    tableName = "Site",
    indices = [Index(value = ["siteURL"], unique = true)]
)
data class Site(
    @PrimaryKey(autoGenerate = true) val siteUID: Int = 0,

    val siteURL: String,

    val title: String? = null,

    @Embedded val largestFavicon: Favicon?
)
