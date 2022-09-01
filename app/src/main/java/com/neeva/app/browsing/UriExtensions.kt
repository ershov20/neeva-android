// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.NeevaConstants

fun String.toSearchUri(neevaConstants: NeevaConstants): Uri {
    return Uri.parse(neevaConstants.appSearchURL)
        .buildUpon()
        .appendQueryParameter("q", this)
        .appendQueryParameter("src", "nvobar")
        .build()
}

fun Uri.isNeevaUri(neevaConstants: NeevaConstants): Boolean {
    return authority == neevaConstants.appHost
}

fun Uri.isNeevaSearchUri(neevaConstants: NeevaConstants): Boolean {
    return toString().startsWith(neevaConstants.appSearchURL)
}
