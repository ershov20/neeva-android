package com.neeva.app.cookiecutter

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CookieEngineMessage<T>(val type: String, val data: T?)

@JsonClass(generateAdapter = true)
data class CookieCuttingPreferences(
    val analytics: Boolean,
    val marketing: Boolean,
    val social: Boolean
)
