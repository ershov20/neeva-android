// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class LoginCallbackIntentParamsTest : BaseTest() {
    @Test
    fun fromLoginCallbackIntent_givenValidString_getsItBack() {
        val intentUri = Uri.Builder()
            .scheme("neeva")
            .authority("login")
            .path("cb")
            .appendQueryParameter("somethingBefore", "whatever")
            .appendQueryParameter("sessionKey", "expectedSession")
            .appendQueryParameter("finalPath", "/")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = LoginCallbackIntentParams.fromLoginCallbackIntent(intent)
        expectThat(result?.sessionKey).isEqualTo("expectedSession")
        expectThat(result?.finalPath).isEqualTo("/")
    }

    @Test
    fun fromLoginCallbackIntent_givenEscapedPath_getsDecodedPath() {
        val intentUri = Uri.Builder()
            .scheme("neeva")
            .authority("login")
            .path("cb")
            .appendQueryParameter("somethingBefore", "whatever")
            .appendQueryParameter("sessionKey", "expectedSession")
            .appendQueryParameter("finalPath", "/%3Fsomewhere")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = LoginCallbackIntentParams.fromLoginCallbackIntent(intent)
        expectThat(result?.sessionKey).isEqualTo("expectedSession")
        expectThat(result?.finalPath).isEqualTo("/?somewhere")
    }

    @Test
    fun fromLoginCallbackIntent_withWrongScheme_returnsNull() {
        val intentUri = Uri.Builder()
            .scheme("https")
            .authority("login")
            .path("cb")
            .appendQueryParameter("somethingBefore", "whatever")
            .appendQueryParameter("sessionKey", "expectedSession")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = LoginCallbackIntentParams.fromLoginCallbackIntent(intent)
        expectThat(result?.sessionKey).isNull()
        expectThat(result?.finalPath).isNull()
        expectThat(result?.retryCode).isNull()
    }

    @Test
    fun fromLoginCallbackIntent_withRetryCode_returnsError() {
        val intentUri = Uri.Builder()
            .scheme("neeva")
            .authority("login")
            .path("cb")
            .appendQueryParameter("retry", "NL016")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = LoginCallbackIntentParams.fromLoginCallbackIntent(intent)
        expectThat(result?.sessionKey).isNull()
        expectThat(result?.finalPath).isNull()
        expectThat(result?.retryCode).isEqualTo("NL016")
    }

    @Test
    fun fromLoginCallbackIntent_withWrongAuthority_returnsNull() {
        val intentUri = Uri.Builder()
            .scheme("neeva")
            .authority("wrongauthority.com")
            .path("cb")
            .appendQueryParameter("somethingBefore", "whatever")
            .appendQueryParameter("sessionKey", "expectedSession")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = LoginCallbackIntentParams.fromLoginCallbackIntent(intent)
        expectThat(result).isNull()
    }
}
