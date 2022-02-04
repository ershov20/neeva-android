package com.neeva.app

import android.content.Intent
import android.net.Uri
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NeevaUserTokenTest : BaseTest() {
    @Test
    fun getToken_resultIsEmpty_returnsNull() {
        val sharedPreferencesModel = mock<SharedPreferencesModel>()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel)
        val result = neevaUserToken.getToken()
        expectThat(result).isNull()
    }

    fun getToken_stringIsSet_returnsString() {
        val sharedPreferencesModel = mock<SharedPreferencesModel> {
            on { getString(any(), eq(NeevaUserToken.KEY_TOKEN), any()) } doReturn "whatever"
        }

        val neevaUserToken = NeevaUserToken(sharedPreferencesModel)
        val result = neevaUserToken.getToken()
        expectThat(result).isEqualTo("whatever")
    }

    @Test
    fun loginCookieString() {
        val sharedPreferencesModel = mock<SharedPreferencesModel> {
            on { getString(any(), eq(NeevaUserToken.KEY_TOKEN), any()) } doReturn "whatever"
        }

        val neevaUserToken = NeevaUserToken(sharedPreferencesModel)
        val result = neevaUserToken.loginCookieString()
        expectThat(result).isEqualTo("${NeevaConstants.loginCookie}=whatever")
    }

    @Test
    fun extractAuthTokenFromIntent_givenValidString_getsItBack() {
        val intentUri = Uri.Builder()
            .scheme("neeva")
            .authority("login")
            .appendQueryParameter("somethingBefore", "whatever")
            .appendQueryParameter("sessionKey", "expectedSession")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = NeevaUserToken.extractAuthTokenFromIntent(intent)
        expectThat(result).isEqualTo("expectedSession")
    }

    @Test
    fun extractAuthTokenFromIntent_withWrongScheme_returnsNull() {
        val intentUri = Uri.Builder()
            .scheme("https")
            .authority("login")
            .appendQueryParameter("somethingBefore", "whatever")
            .appendQueryParameter("sessionKey", "expectedSession")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = NeevaUserToken.extractAuthTokenFromIntent(intent)
        expectThat(result).isNull()
    }

    @Test
    fun extractAuthTokenFromIntent_withWrongAuthority_returnsNull() {
        val intentUri = Uri.Builder()
            .scheme("neeva")
            .authority("wrongauthority.com")
            .appendQueryParameter("somethingBefore", "whatever")
            .appendQueryParameter("sessionKey", "expectedSession")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        val result = NeevaUserToken.extractAuthTokenFromIntent(intent)
        expectThat(result).isNull()
    }

    @Test
    fun setToken() {
        val sharedPreferencesModel = mock<SharedPreferencesModel>()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel)

        neevaUserToken.setToken("expectedToken")
        verify(sharedPreferencesModel).setValue(
            eq(SharedPrefFolder.USER),
            eq(NeevaUserToken.KEY_TOKEN),
            eq("expectedToken")
        )
    }

    @Test
    fun removeToken() {
        val sharedPreferencesModel = mock<SharedPreferencesModel>()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel)

        neevaUserToken.removeToken()
        verify(sharedPreferencesModel).removeValue(
            eq(SharedPrefFolder.USER),
            eq(NeevaUserToken.KEY_TOKEN)
        )
    }
}
