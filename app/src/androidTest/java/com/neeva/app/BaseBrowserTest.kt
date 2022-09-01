// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.testcommon.WebpageServingRule
import org.junit.Rule

/** Base class for tests that need to be served with fake webpages. */
abstract class BaseBrowserTest : BaseHiltTest() {
    @get:Rule
    val webpageServingRule = WebpageServingRule()

    override fun setUp() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                InstrumentationRegistry.getInstrumentation().targetContext,
                "Starting ${this::class.simpleName}",
                Toast.LENGTH_SHORT
            ).show()
        }
        super.setUp()
    }
}
