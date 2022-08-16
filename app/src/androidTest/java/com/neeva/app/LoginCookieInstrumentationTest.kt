package com.neeva.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData
import com.neeva.app.userdata.NeevaUserToken
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrEmpty

@HiltAndroidTest
class LoginCookieInstrumentationTest : BaseHiltTest() {
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var neevaUserToken: NeevaUserToken
    private lateinit var neevaUser: NeevaUser
    private lateinit var sharedPreferencesModel: SharedPreferencesModel

    @get:Rule
    val skipNeevaScopeTooltipRule =
        PresetSharedPreferencesRule(skipNeevaScopeTooltip = true)

    private fun setUpLoggedInUser(context: Context) {
        neevaConstants = TestNeevaConstantsModule.neevaConstants
        sharedPreferencesModel = SharedPreferencesModel(context)
        neevaUserToken = NeevaUserToken(sharedPreferencesModel, neevaConstants)
        neevaUserToken.setToken("myToken")
        val data = NeevaUserData(
            "my-id",
            "some display name",
            "email@neeva.co",
            Uri.parse("https://www.cdn/my-image.png"),
            NeevaUser.SSOProvider.GOOGLE
        )
        neevaUser = NeevaUser(data, neevaUserToken)
    }

    private fun onActivityStartedTest(context: Context, test: (activity: NeevaActivity) -> Unit) {
        val intent = Intent.makeMainActivity(ComponentName(context, NeevaActivity::class.java))
        ActivityScenario.launch<NeevaActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity: NeevaActivity ->
                test(activity)
            }
            scenario.close()
        }
    }

    @Test
    fun signedIn_whenLoggedIn_UserDataClearedAndNeevaUserTokenAndLoginCookieExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setUpLoggedInUser(context)

        onActivityStartedTest(context) { activity ->
            // because NeevaUser.fetch() is run on a fake token ("myToken")
            // ApolloWrapper should give a null response and ONLY clear the NeevaUser.data.
            // The token + cookies should still be set to "myToken".
            activity.webLayerModel.currentBrowser
                .getCookiePairs(Uri.parse(neevaConstants.appURL)) {
                    expectThat(
                        it.find { cookiePair -> cookiePair.key == neevaConstants.loginCookie }
                            ?.value
                    ).isEqualTo("myToken")
                }
            expectThat(activity.neevaUser.neevaUserToken.getToken()).isEqualTo("myToken")
            expectThat(activity.neevaUser.data).isEqualTo(NeevaUserData())
        }
    }

    @Test
    fun signOut_whenLoggedIn_clearsUserAndNeevaUserTokenAndCookies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setUpLoggedInUser(context)

        onActivityStartedTest(context) { activity ->
            activity.activityViewModel.signOut()

            // ^^ sign out should have cleared NeevaUser.data and NeevaUserToken!
            expectThat(activity.neevaUser.neevaUserToken.getToken()).isEmpty()
            expectThat(activity.neevaUser.data).isEqualTo(NeevaUserData())

            activity.webLayerModel.currentBrowser
                .getCookiePairs(Uri.parse(neevaConstants.appURL)) {
                    expectThat(
                        it.find {
                            cookiePair ->
                            cookiePair.key == neevaConstants.loginCookie
                        }?.value
                    ).isNullOrEmpty()
                }
        }
    }
}
