// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage

import com.neeva.app.BaseHiltTest
import javax.inject.Inject
import org.junit.After

abstract class HistoryDatabaseBaseTest : BaseHiltTest() {
    @Inject lateinit var database: HistoryDatabase

    @After
    open fun tearDown() {
        database.close()
    }
}
