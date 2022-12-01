// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.neeva.app.BuildConfig
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.ui.PopupModel
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.security.MessageDigest
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.IOException

open class OktaSignUpHandler(
    private val neevaConstants: NeevaConstants,
) {
    protected open val createOktaAccountURL: String = neevaConstants.createOktaAccountURL
    protected open val onLoginCookieReceivedUrl: String = "neeva://login/cb?sessionKey="

    suspend fun createOktaAccount(
        activityContext: Context,
        popupModel: PopupModel,
        emailProvided: String,
        passwordProvided: String = "",
        marketingEmailOptOut: Boolean = false,
    ) {
        val salt = generateSalt()
        val passwordSaltedAndEncoded = (salt + passwordProvided).sha512().base64()
        val requestParams = OktaSignupRequestParams(
            email = emailProvided,
            password = passwordSaltedAndEncoded,
            salt = salt.encodeUtf8().base64(),
            marketingEmailOptOut = marketingEmailOptOut
        )
        val moshi = Moshi.Builder().build()
        val jsonAdapter: JsonAdapter<OktaSignupRequestParams> =
            moshi.adapter(OktaSignupRequestParams::class.java)
        val requestBody = jsonAdapter.toJson(requestParams).toRequestBody()
        val request = Request.Builder()
            .method("POST", requestBody)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Neeva-Client-ID", neevaConstants.browserIdentifier)
            .addHeader("X-Neeva-Client-Version", BuildConfig.VERSION_NAME)
            .url(createOktaAccountURL)
            .build()
        val cookieJar = FirstRunCookieJar()
        val client = OkHttpClient.Builder().cookieJar(cookieJar).build()

        var errorMessageResId: Int? = null
        var loginCookieValue: String? = null
        try {
            client.newCall(request).execute().use { response ->
                cookieJar.authCookie(loginCookie = neevaConstants.loginCookieKey)?.let { cookie ->
                    loginCookieValue = cookie.value
                    return@use
                }

                errorMessageResId = R.string.generic_signup_error
                if (!response.isSuccessful) {
                    errorMessageResId = when (
                        moshi
                            .adapter(OktaSignupErrorResponse::class.java)
                            .fromJson(response.body?.string() ?: "")
                            ?.error
                    ) {
                        "UsedEmail" -> R.string.used_email_error
                        "InvalidEmail" -> R.string.invalid_email_error
                        "InvalidRequest" -> R.string.invalid_request_error
                        "InvalidToken" -> R.string.invalid_token_error
                        "UsedToken" -> R.string.invalid_token_error
                        else -> R.string.generic_signup_error
                    }
                }
            }
        } catch (e: IOException) {
            errorMessageResId = R.string.generic_signup_error
        }

        errorMessageResId?.let { popupModel.showSnackbar(activityContext.getString(it)) }

        loginCookieValue?.let {
            // Fire an Intent that will allow NeevaActivity to process the result of the sign up the
            // same way the other login flows pass cookies in.
            val intent = Intent()
                .setAction(Intent.ACTION_VIEW)
                .setClass(activityContext, NeevaActivity::class.java)
                .setData(Uri.parse("$onLoginCookieReceivedUrl$loginCookieValue"))
            activityContext.startActivity(intent)
        }
    }

    private fun generateSalt() =
        buildString {
            (1..12).forEach { _ ->
                this.append(
                    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random()
                )
            }
        }

    private fun String.sha512() =
        ByteString.of(*MessageDigest.getInstance("SHA-512").digest(this.toByteArray()))
}

@JsonClass(generateAdapter = true)
data class OktaSignupRequestParams(
    val email: String,
    val firstname: String = "Member",
    val lastname: String = "",
    val password: String,
    val salt: String,
    val visitorID: String = "",
    val expVisitorID: String = "",
    val expVisitorOverrides: String = "",
    val emailSubmissionID: String = "",
    val referralCode: String = "",
    val marketingEmailOptOut: Boolean,
    val ignoreCountryCode: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OktaSignupErrorResponse(
    val error: String
)

class FirstRunCookieJar : CookieJar {
    internal var cookies: List<Cookie> = emptyList()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return mutableListOf()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies = cookies
    }

    fun authCookie(loginCookie: String) = cookies.find { it.name == loginCookie }
}
