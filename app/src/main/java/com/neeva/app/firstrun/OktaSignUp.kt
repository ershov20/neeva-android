package com.neeva.app.firstrun

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.neeva.app.BuildConfig
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.userdata.NeevaUserToken
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

object OktaSignUp {
    suspend fun createOktaAccount(
        activityContext: Context,
        snackbarModel: SnackbarModel,
        neevaUserToken: NeevaUserToken,
        emailProvided: String,
        passwordProvided: String = "",
        marketingEmailOptOut: Boolean = false
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
            .addHeader("X-Neeva-Client-ID", NeevaConstants.browserIdentifier)
            .addHeader("X-Neeva-Client-Version", BuildConfig.VERSION_NAME)
            .url(NeevaConstants.createOktaAccountURL)
            .build()
        val cookieJar = FirstRunCookieJar()
        val client = OkHttpClient.Builder().cookieJar(cookieJar).build()
        val response = client.newCall(request).execute()

        cookieJar
            .authCookie()
            ?.let { cookie ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("neeva://login/cb?sessionKey=${cookie.value}")
                activityContext.startActivity(intent)
            }
        if (!response.isSuccessful) {
            val errorMsg = when (
                moshi
                    .adapter(OktaSignupErrorResponse::class.java)
                    .fromJson(response.body?.string() ?: "")?.error
            ) {
                "UsedEmail" -> {
                    activityContext.getString(R.string.used_email_error)
                }
                "InvalidEmail" -> {
                    activityContext.getString(R.string.invalid_email_error)
                }
                "InvalidRequest" -> {
                    activityContext.getString(R.string.invalid_request_error)
                }
                "InvalidToken" -> {
                    activityContext.getString(R.string.invalid_token_error)
                }
                "UsedToken" -> {
                    activityContext.getString(R.string.invalid_token_error)
                }
                else -> {
                    activityContext.getString(R.string.generic_signup_error)
                }
            }
            snackbarModel.show(errorMsg)
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
    lateinit var cookies: List<Cookie>
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return mutableListOf()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies = cookies
    }

    fun authCookie() = cookies.find { it.name == NeevaConstants.loginCookie }
}
