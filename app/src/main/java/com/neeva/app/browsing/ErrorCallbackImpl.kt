// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import org.chromium.weblayer.ErrorPage
import org.chromium.weblayer.ErrorPageCallback
import org.chromium.weblayer.Navigation

class ErrorCallbackImpl(
    private val activityCallbackProvider: ActivityCallbackProvider
) : ErrorPageCallback() {
    override fun onBackToSafety(): Boolean {
        activityCallbackProvider.get()?.onBackPressed()
        return true
    }

    // https://github.com/neevaco/neeva-android/issues/77
    // Even WebLayerShellActivity shows a blank white screen.
    override fun getErrorPage(navigation: Navigation): ErrorPage? = null
}
