package com.neeva.app

import android.net.Uri
import okhttp3.Cookie

object NeevaConstants {
    val appHost: String = "neeva.com"
    val appURL: String = "https://$appHost/"
    val appSearchURL: String = "${appURL}search"
    val appSpacesURL: String = "${appURL}spaces"
    val appConnectionsURL: String = "${appURL}connections"

    val appSettingsURL: String = "${appURL}settings"
    val appReferralURL: String = "${appURL}settings/referrals"
    val appManageMemory: String = "${appURL}settings#memory-mode"

    val appPrivacyURL: String = "${appURL}privacy"
    val appTermsURL: String = "${appURL}terms"

    val appWelcomeToursURL: String = "$appURL#modal-hello"
    val appHelpCenterURL: String = "https://help.$appHost/"

    val playStoreUri: Uri = Uri.parse("https://play.google.com/store/apps/details?id=com.neeva.app")

    val loginCookie: String = "httpd~login"
    val browserTypeCookie = Cookie.Builder()
        .name("BrowserType")
        .secure()
        .domain(appHost)
        .expiresAt(Long.MAX_VALUE)
        .value("neeva-android")
        .build()
    val browserVersionCookie = Cookie.Builder()
        .name("BrowserVersion")
        .secure()
        .domain(appHost)
        .expiresAt(Long.MAX_VALUE)
        .value(NeevaBrowser.versionString ?: "0.0.1")
        .build()
}
