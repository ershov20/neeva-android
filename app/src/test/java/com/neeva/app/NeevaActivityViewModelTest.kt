package com.neeva.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class NeevaActivityViewModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var neevaActivity: NeevaActivity
    @Mock private lateinit var neevaUser: NeevaUser
    @Mock private lateinit var spaceStore: SpaceStore
    @Mock private lateinit var browserWrapper: BrowserWrapper
    @Mock private lateinit var snackbarModel: SnackbarModel

    private lateinit var webLayerModel: WebLayerModel
    private lateinit var neevaActivityViewModel: NeevaActivityViewModel

    private lateinit var dispatchers: Dispatchers

    override fun setUp() {
        super.setUp()

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )

        webLayerModel = mock {
            on { currentBrowser } doReturn browserWrapper
        }

        neevaActivityViewModel = NeevaActivityViewModel(
            pendingLaunchIntent = null,
            neevaUser = neevaUser,
            webLayerModel = webLayerModel,
            snackbarModel = snackbarModel,
            spaceStore = spaceStore,
            dispatchers = dispatchers
        )
    }

    @Test
    fun signOut() {
        neevaActivityViewModel.signOut()
        coroutineScopeRule.scope.advanceUntilIdle()

        runBlocking {
            verify(spaceStore).deleteAllData()
        }
        verify(neevaUser).clearUser()
        verify(webLayerModel).clearNeevaCookies()
    }

    @Test
    fun fireExternalIntentForUri_givenHttpsScheme_firesIntentAndClosesTab() {
        val uri = "https://www.example.com"

        neevaActivityViewModel.fireExternalIntentForUri(neevaActivity, Uri.parse(uri), true)

        val intentCaptor = argumentCaptor<Intent>()
        verify(neevaActivity).startActivity(intentCaptor.capture())

        val actualIntent = intentCaptor.lastValue
        val expectedIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        expectThat(actualIntent.action).isEqualTo(expectedIntent.action)
        expectThat(actualIntent.data).isEqualTo(expectedIntent.data)

        verify(snackbarModel, never()).show(any(), eq(null), any(), any(), any())
        verify(neevaActivity).onBackPressed()
    }

    @Test
    fun fireExternalIntentForUri_givenIntentScheme_firesIntentAndClosesTab() {
        val uri = "intent://www.netflix.com/title/81333845" +
            "#Intent;scheme=https;package=com.netflix.mediaclient;" +
            "S.browser_fallback_url=" +
            "https%3A%2F%2Fwww.netflix.com%2Ftitle%2F81333845%3FpreventIntent%3Dtrue;end"

        neevaActivityViewModel.fireExternalIntentForUri(neevaActivity, Uri.parse(uri), true)

        val intentCaptor = argumentCaptor<Intent>()
        verify(neevaActivity).startActivity(intentCaptor.capture())

        val actualIntent = intentCaptor.lastValue
        val expectedIntent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
        expectThat(actualIntent.action).isEqualTo(expectedIntent.action)
        expectThat(actualIntent.data).isEqualTo(expectedIntent.data)

        verify(snackbarModel, never()).show(any(), eq(null), any(), any(), any())
        verify(neevaActivity).onBackPressed()
    }

    @Test
    fun fireExternalIntentForUri_givenIntentSchemeWithoutActivity_doesNothing() {
        val uri = "intent://www.netflix.com/title/81333845" +
            "#Intent;scheme=https;package=com.netflix.mediaclient;end"

        // Indicate that there's no Activity to handle the Intent.
        Mockito.`when`(neevaActivity.startActivity(any())).doThrow(ActivityNotFoundException())
        Mockito.`when`(neevaActivity.getString(any(), any())).doReturn("error string")

        neevaActivityViewModel.fireExternalIntentForUri(neevaActivity, Uri.parse(uri), true)

        val intentCaptor = argumentCaptor<Intent>()
        verify(neevaActivity).startActivity(intentCaptor.capture())

        val actualIntent = intentCaptor.lastValue
        val expectedIntent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
        expectThat(actualIntent.action).isEqualTo(expectedIntent.action)
        expectThat(actualIntent.data).isEqualTo(expectedIntent.data)

        verify(snackbarModel, never()).show(any(), eq(null), any(), any(), any())
        verify(neevaActivity, never()).onBackPressed()
    }

    @Test
    fun fireExternalIntentForUri_givenInvalidURI_showsError() {
        val uri = "intent://www.netflix.com/title/81333845" +
            "#Intent;scheme=https;package=com.netflix.mediaclient"

        // Indicate that there's no Activity to handle the Intent.
        Mockito.`when`(neevaActivity.startActivity(any())).doThrow(ActivityNotFoundException())
        Mockito.`when`(neevaActivity.getString(any(), any())).doReturn("error string")

        neevaActivityViewModel.fireExternalIntentForUri(neevaActivity, Uri.parse(uri), true)

        verify(neevaActivity, never()).startActivity(any())
        verify(snackbarModel).show(any(), eq(null), any(), any(), any())
    }
}
