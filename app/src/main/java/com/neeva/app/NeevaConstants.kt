package com.neeva.app

import okhttp3.Cookie

object NeevaConstants {
    var appHost: String = "neeva.com"
    var appURL: String = "https://$appHost/"
    var appMarketingURL: String = appURL
    var appSearchURL: String = "${appURL}search"
    var appSpacesURL: String = "${appURL}spaces"
    var appConnectionsURL: String = "${appURL}connections"
    var appSettingsURL: String = "${appURL}settings"
    var appReferralURL: String = "${appSettingsURL}referrals"
    var appPrivacyURL: String = "${appMarketingURL}privacy"
    var appTermsURL: String = "${appMarketingURL}terms"
    var appWelcomeToursURL: String = "$appURL#modal-hello"
    var appHelpCenterURL: String = "https://help.$appHost/"
    var appManageMemory: String = "${appMarketingURL}settings#memory-mode"

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
