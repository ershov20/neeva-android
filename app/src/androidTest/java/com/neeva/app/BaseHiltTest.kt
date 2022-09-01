// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import androidx.annotation.CallSuper
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Before
import org.junit.Rule

/** Base class for instrumentation tests.  Subclasses must be annotated with @HiltAndroidTest. */
abstract class BaseHiltTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    @CallSuper
    open fun setUp() {
        hiltRule.inject()
    }
}
