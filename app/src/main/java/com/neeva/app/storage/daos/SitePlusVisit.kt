// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
