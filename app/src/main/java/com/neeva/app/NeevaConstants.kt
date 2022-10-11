// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.net.Uri
import android.os.Environment
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cookie

open class NeevaConstants(
    val appHost: String = "neeva.com",
    val appURL: String = "https://$appHost/",
    val appHelpCenterURL: String = "https://help.$appHost/"
) {
    val appWelcomeToursURL: String = "$appURL#modal-hello"
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

    open val contentFilterLearnMoreUrl: String = "$appHelpCenterURL/hc/en-us/articles/4486326606355"
    val createOktaAccountURL: String = "${appURL}login/create"

    val playStoreUri: Uri = Uri.parse("https://play.google.com/store/apps/details?id=com.neeva.app")

    open val downloadDirectory: File by lazy {
        Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
    }

    /** Identifies the Android client when making backend requests. */
    val browserIdentifier = "co.neeva.app.android.browser"

    val loginCookieKey: String = "httpd~login"
    val incognitoCookieKey: String = "httpd~incognito"
    val previewCookieKey: String = "httpd~preview"

    val browserTypeCookie: Cookie by lazy {
        createPersistentNeevaOkHttpCookie(
            cookieName = "BrowserType",
            cookieValue = "neeva-android",
            isSessionToken = false
        )
    }
    val browserTypeCookieString: String by lazy {
        createPersistentNeevaCookieString(
            cookieName = "BrowserType",
            cookieValue = "neeva-android",
            isSessionToken = false
        )
    }
    val browserVersionCookie: Cookie by lazy {
        createPersistentNeevaOkHttpCookie(
            cookieName = "BrowserVersion",
            cookieValue = BuildConfig.VERSION_NAME,
            isSessionToken = false
        )
    }
    val browserVersionCookieString: String by lazy {
        createPersistentNeevaCookieString(
            cookieName = "BrowserVersion",
            cookieValue = BuildConfig.VERSION_NAME,
            isSessionToken = false
        )
    }

    fun createPersistentNeevaOkHttpCookie(
        cookieName: String,
        cookieValue: String,
        isSessionToken: Boolean,
        durationMinutes: Int = Int.MAX_VALUE
    ): Cookie {
        return Cookie.Builder()
            .name(cookieName)
            .value(cookieValue)
            .expiresAt(
                TimeUnit.MINUTES.toMillis(durationMinutes.toLong()) + System.currentTimeMillis()
            )
            .secure()
            .apply {
                // The website requires session tokens to be set as "HttpOnly".
                if (isSessionToken) {
                    httpOnly()
                    hostOnlyDomain(appHost)
                } else {
                    domain(appHost)
                }
            }
            .build()
    }

    open fun createPersistentNeevaCookieString(
        cookieName: String,
        cookieValue: String,
        isSessionToken: Boolean,
        durationMinutes: Int = Int.MAX_VALUE
    ): String {
        return createPersistentNeevaOkHttpCookie(
            cookieName = cookieName,
            cookieValue = cookieValue,
            isSessionToken = isSessionToken,
            durationMinutes = durationMinutes
        ).toString()
    }
}
