package com.neeva.app.userdata

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.UserInfoQuery
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
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

    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper
    private lateinit var dispatchers: Dispatchers
    private lateinit var mockWebLayerModel: WebLayerModel
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var neevaUserToken: NeevaUserToken
    private lateinit var neevaUserData: NeevaUserData
    private lateinit var neevaUser: NeevaUser
    private lateinit var sharedPreferencesModel: SharedPreferencesModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    override fun setUp() {
        super.setUp()
        neevaConstants = NeevaConstants()
        setUpLoggedInUser(ApplicationProvider.getApplicationContext())
        setUpMockWeblayerModel()
        apolloWrapper = TestAuthenticatedApolloWrapper(
            neevaUserToken = neevaUserToken,
            neevaConstants = neevaConstants
        )
        coroutineScopeRule.scope.advanceUntilIdle()
    }

    private fun setUpLoggedInUser(context: Context) {
        sharedPreferencesModel = SharedPreferencesModel(context)
        neevaUserToken = NeevaUserToken(sharedPreferencesModel, neevaConstants)
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
    fun isSignedOut_whenSignedIn_returnsFalse() {
        expectThat(neevaUser.isSignedOut()).isEqualTo(false)
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
        apolloWrapper.registerTestResponse(UserInfoQuery(), USER_RESPONSE)
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
        val USER_RESPONSE = UserInfoQuery.Data(
            user = UserInfoQuery.User(
                id = "response_id",
                profile = UserInfoQuery.Profile(
                    displayName = "response_displayName",
                    email = "response_email",
                    pictureURL = "response_pictureUrl"
                ),
                flags = emptyList(),
                featureFlags = emptyList(),
                authProvider = null,
                subscriptionType = null
            )
        )
    }
}
