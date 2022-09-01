// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.chromium.weblayer.Callback
import org.chromium.weblayer.WebLayer

class WebLayerFactory(@ApplicationContext private val appContext: Context) {
    fun load(callback: Callback<WebLayer>) = WebLayer.loadAsync(appContext, callback)
}
