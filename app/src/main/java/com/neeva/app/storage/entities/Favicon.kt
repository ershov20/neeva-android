// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage.entities

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.storage.toLetterBitmap

/** Stores information about a website's favicon. */
data class Favicon(
    val faviconURL: String?,
    val width: Int,
    val height: Int,
) {
    companion object {
        /** Generates a single-colored Bitmap from the given Uri. */
        fun Uri?.toBitmap(): Bitmap {
            val backgroundColor = hashCode().or(0xff000000.toInt())
            val text: String = this?.authority?.takeIf { it.isNotEmpty() }?.take(1) ?: ""
            return text.toLetterBitmap(0.75f, backgroundColor)
        }
    }
}
