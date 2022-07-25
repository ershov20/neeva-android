package com.neeva.app.cookiecutter

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CookieEngineMessage<T>(val type: String, val data: T?)

@JsonClass(generateAdapter = true)
data class CookieCuttingPreferences(
    val analytics: Boolean,
    val marketing: Boolean,
    val social: Boolean
) {
    companion object {
        fun fromSet(prefs: Set<CookieCutterModel.CookieNoticeCookies>): CookieCuttingPreferences {
            return CookieCuttingPreferences(
                prefs.contains(CookieCutterModel.CookieNoticeCookies.ANALYTICS),
                prefs.contains(CookieCutterModel.CookieNoticeCookies.MARKETING),
                prefs.contains(CookieCutterModel.CookieNoticeCookies.SOCIAL)
            )
        }
    }
}
