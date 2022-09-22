// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.net.Uri
import okhttp3.Cookie

open class NeevaConstants(
    val appHost: String = "neeva.com",
    val appURL: String = "https://$appHost/",
    val cookieHost: String = appHost,
    val cookieURL: String = "https://$cookieHost/",
    val appHelpCenterURL: String = "https://help.$appHost/",
    val appWelcomeToursURL: String = "$appURL#modal-hello"
) {
    val appSearchURL: String = "${appURL}search"
    val appSpacesURL: String = "${appURL}spaces"
    val appConnectionsURL: String = "${appURL}connections"

    val appSettingsURL: String = "${appURL}settings"
    val appReferralURL: String = "${appURL}settings/referrals"
    val appManageMemory: String = "${appURL}settings#memory-mode"
    val appMembershipURL: String = "${appURL}settings/membership"

    val appPrivacyURL: String = "${appURL}privacy"
    val appTermsURL: String = "${appURL}terms"

    val apolloURL: String = "${appURL}graphql"

    /** Endpoint for creating new incognito session tokens. */
    val incognitoURL: String = "${appURL}incognito/create-session"

    /** Endpoint for creating new preview session tokens. */
    val previewCookieURL: String = "${appURL}preview/create-session"

    open val cookieCutterLearnMoreUrl: String = "$appHelpCenterURL/hc/en-us/articles/4486326606355"
    val createOktaAccountURL: String = "${appURL}login/create"

    val playStoreUri: Uri = Uri.parse("https://play.google.com/store/apps/details?id=com.neeva.app")

    /** Identifies the Android client when making backend requests. */
    val browserIdentifier = "co.neeva.app.android.browser"

    val loginCookie: String = "httpd~login"
    val incognitoCookie: String = "httpd~incognito"
    val previewCookie: String = "httpd~preview"

    val browserTypeCookie = createNeevaCookie(
        cookieName = "BrowserType",
        cookieValue = "neeva-android"
    )

    val browserVersionCookie = createNeevaCookie(
        cookieName = "BrowserVersion",
        cookieValue = BuildConfig.VERSION_NAME
    )

    fun createLoginCookie(cookieValue: String) = createNeevaCookie(
        cookieName = loginCookie,
        cookieValue = cookieValue
    )

    fun createNeevaCookie(cookieName: String, cookieValue: String): Cookie {
        // TODO(dan.alcantara): |expiresAt| _should_ be set to the actual expiration of the cookie
        //                      but the app doesn't keep track of them.
        return Cookie.Builder()
            .name(cookieName)
            .value(cookieValue)
            .secure()
            .domain(cookieHost)
            .expiresAt(Long.MAX_VALUE)
            .build()
    }
}
