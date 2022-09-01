// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
