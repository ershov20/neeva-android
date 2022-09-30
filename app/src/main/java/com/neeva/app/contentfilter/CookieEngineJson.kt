// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CookieEngineMessage<T>(val type: String, val data: T?)

@JsonClass(generateAdapter = true)
data class ContentFilteringPreferences(
    val analytics: Boolean,
    val marketing: Boolean,
    val social: Boolean
) {
    companion object {
        fun fromSet(prefs: Set<ContentFilterModel.CookieNoticeCookies>):
            ContentFilteringPreferences {
            return ContentFilteringPreferences(
                prefs.contains(ContentFilterModel.CookieNoticeCookies.ANALYTICS),
                prefs.contains(ContentFilterModel.CookieNoticeCookies.MARKETING),
                prefs.contains(ContentFilterModel.CookieNoticeCookies.SOCIAL)
            )
        }
    }
}
