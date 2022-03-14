package com.neeva.app.userdata

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.TestApolloWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class NeevaUserTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var dispatchers: Dispatchers
    private lateinit var sharedPreferencesModel: SharedPreferencesModel
    private lateinit var neevaUserToken: NeevaUserToken
    private lateinit var neevaUserData: NeevaUserData
    private lateinit var neevaUser: NeevaUser
    private lateinit var mockWebLayerModel: WebLayerModel
    private lateinit var apolloWrapper: TestApolloWrapper

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    override fun setUp() {
        super.setUp()
        setUpLoggedInUser(ApplicationProvider.getApplicationContext())
        setUpMockWeblayerModel()
        apolloWrapper = TestApolloWrapper(neevaUserToken = neevaUserToken)
        coroutineScopeRule.scope.advanceUntilIdle()
    }

    private fun setUpLoggedInUser(context: Context) {
        sharedPreferencesModel = SharedPreferencesModel(context)
        neevaUserToken = NeevaUserToken(sharedPreferencesModel)
        neevaUserToken.setToken("myToken")
        neevaUserData = NeevaUserData(
            "my-id",
            "some display name",
            "email@neeva.co",
            Uri.parse("https://www.cdn/my-image.png"),
            NeevaUser.SSOProvider.GOOGLE
        )
        neevaUser = NeevaUser(neevaUserData, neevaUserToken)
    }

    private fun setUpMockWeblayerModel() {
        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )
        mockWebLayerModel = mock {}
    }

    @Test
    fun clearUser_dataIsEmpty_clearsNeevaUserData() {
        val neevaUser = NeevaUser(NeevaUserData(), neevaUserToken)
        neevaUser.clearUser()
        expectThat(neevaUser.data).isEqualTo(NeevaUserData())
    }

    @Test
    fun clearUser_dataIsNotEmpty_clearsNeevaUserData() {
        neevaUser.clearUser()
        expectThat(neevaUser.data).isEqualTo(NeevaUserData())
    }

    @Test
    fun signOut_nullWeblayerModel_clearsUserData() {
        neevaUser.signOut(null)
        expectThat(neevaUser.data).isEqualTo(NeevaUserData())
        verify(mockWebLayerModel, times(0)).clearNeevaCookies()
    }

    @Test
    fun signOut_withWeblayerModel_clearsUser() {
        neevaUser.signOut(mockWebLayerModel)
        verify(mockWebLayerModel).clearNeevaCookies()
    }

    @Test
    fun isSignedOut_whenSignedIn_returnsFalse() {
        expectThat(neevaUser.isSignedOut()).isEqualTo(false)
    }

    @Test
    fun isSignedOut_whenSignedOut_returnsTrue() {
        neevaUser.signOut(mockWebLayerModel)
        expectThat(neevaUser.isSignedOut()).isEqualTo(true)
    }

    @Test
    fun fetch_badResponse_clearsUser() {
        coroutineScopeRule.scope.advanceUntilIdle()
        runBlocking {
            neevaUser.fetch(apolloWrapper)
        }
        expectThat(neevaUser.data).isEqualTo(NeevaUserData())
    }

    @Test
    fun fetch_goodResponse_setsUser() {
        apolloWrapper.addResponse(FETCH_RESPONSE)
        coroutineScopeRule.scope.advanceUntilIdle()
        runBlocking {
            neevaUser.fetch(apolloWrapper)
        }

        expectThat(neevaUser.data).isEqualTo(
            NeevaUserData(
                id = "response_id",
                displayName = "response_displayName",
                email = "response_email",
                pictureURI = Uri.parse("response_pictureUrl"),
                ssoProvider = NeevaUser.SSOProvider.UNKNOWN
            )
        )
    }

    companion object {
        val FETCH_RESPONSE = """
            {
                "data": {
                    "user": {
                        "id":"response_id",
                        "profile": {
                            "displayName": "response_displayName",
                            "email": "response_email",
                            "pictureURL": "response_pictureUrl"
                        },
                        "flags": [],
                        "featureFlags": []
                    }
                }
            }
        """.trimIndent()
    }
}
