package com.neeva.app

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
    var appHelpCenterURL: String = "https://help.neeva.com/"

    val loginCookie: String = "httpd~login"
}
