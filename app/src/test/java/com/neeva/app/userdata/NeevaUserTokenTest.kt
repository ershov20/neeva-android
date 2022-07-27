package com.neeva.app.userdata

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.NeevaConstants
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class NeevaUserTokenTest : BaseTest() {
    @Test
    fun getToken_resultIsEmpty_returnsEmpty() {
        val sharedPreferencesModel = mock<SharedPreferencesModel> {
            on { getValue(any(), eq(SharedPrefFolder.User.Token), eq("")) } doReturn ""
        }
        val neevaConstants = NeevaConstants()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel, neevaConstants)
        val result = neevaUserToken.getToken()
        expectThat(result).isEmpty()
    }

    @Test
    fun getToken_stringIsSet_returnsString() {
        val sharedPreferencesModel = mock<SharedPreferencesModel> {
            on {
                getValue(any(), eq(SharedPrefFolder.User.Token), any() as String)
            } doReturn "whatever"
        }
        val neevaConstants = NeevaConstants()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel, neevaConstants)
        val result = neevaUserToken.getToken()
        expectThat(result).isEqualTo("whatever")
    }

    @Test
    fun loginCookieString() {
        val sharedPreferencesModel = mock<SharedPreferencesModel> {
            on {
                getValue(any(), eq(SharedPrefFolder.User.Token), any() as String)
            } doReturn "whatever"
        }

        val neevaConstants = NeevaConstants()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel, neevaConstants)
        val result = neevaUserToken.loginCookieString()
        expectThat(result).isEqualTo("${neevaConstants.loginCookie}=whatever")
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
        val neevaConstants = NeevaConstants()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel, neevaConstants)

        neevaUserToken.setToken("expectedToken")
        verify(sharedPreferencesModel).setValue(
            eq(SharedPrefFolder.User),
            eq(SharedPrefFolder.User.Token),
            eq("expectedToken"),
            eq(true)
        )
    }

    @Test
    fun removeToken() {
        val sharedPreferencesModel = mock<SharedPreferencesModel>()
        val neevaConstants = NeevaConstants()
        val neevaUserToken = NeevaUserToken(sharedPreferencesModel, neevaConstants)

        neevaUserToken.removeToken()
        verify(sharedPreferencesModel).removeValue(
            eq(SharedPrefFolder.User),
            eq(SharedPrefFolder.User.Token)
        )
    }
}
