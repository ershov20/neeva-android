package com.neeva.app.firstrun

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Tasks
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.NeevaConstants
import com.neeva.app.logging.ClientLogger
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUserToken
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirstRunModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var context: Context
    private lateinit var firstRunModel: FirstRunModel
    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper

    override fun setUp() {
        super.setUp()

        val neevaConstants = NeevaConstants()

        context = ApplicationProvider.getApplicationContext()
        val sharedPreferencesModel = SharedPreferencesModel(context)
        val neevaUserToken = NeevaUserToken(
            sharedPreferencesModel = sharedPreferencesModel,
            neevaConstants = neevaConstants
        )

        apolloWrapper = TestAuthenticatedApolloWrapper(
            neevaUserToken = neevaUserToken,
            neevaConstants = neevaConstants
        )
        val clientLogger = ClientLogger(
            apolloWrapper,
            coroutineScopeRule.scope,
            coroutineScopeRule.dispatchers,
            neevaConstants,
            sharedPreferencesModel
        )
        val popupModel = PopupModel(coroutineScopeRule.scope, coroutineScopeRule.dispatchers)

        val signInAccount = mock<GoogleSignInAccount> {
            on { idToken } doReturn "valid_token"
            on { serverAuthCode } doReturn "valid_code"
        }

        firstRunModel = FirstRunModel(
            sharedPreferencesModel = sharedPreferencesModel,
            neevaUserToken = neevaUserToken,
            neevaConstants = neevaConstants,
            clientLogger = clientLogger,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            popupModel = popupModel,
            googleSignInAccountProvider = { Tasks.forResult(signInAccount) }
        )
    }

    @Test fun handleLoginActivityResult_onSuccessCalledWithValidResult() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            // Mock a GoogleSignInAccount that will return valid results for success and set it.
            val intent = Intent()
            val activityResult = mock<ActivityResult> {
                on { resultCode } doReturn Activity.RESULT_OK
                on { data } doReturn intent
            }

            var onSuccessCalled = false
            val onSuccess = { _: Uri -> onSuccessCalled = true }

            firstRunModel.handleLoginActivityResult(
                context, activityResult, onSuccess = onSuccess
            )

            expectThat(onSuccessCalled).isTrue()
        }

    @Test fun handleLoginActivityResult_failedResultDoesntCallOnSuccess() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            val activityResult = mock<ActivityResult> {
                on { resultCode } doReturn Activity.RESULT_CANCELED
            }

            var onSuccessCalled = false
            val onSuccess = { _: Uri -> onSuccessCalled = true }

            firstRunModel.handleLoginActivityResult(context, activityResult, onSuccess = onSuccess)

            expectThat(onSuccessCalled).isFalse()
        }
}
