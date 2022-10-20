// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class NeevaActivityViewModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var loginToken: LoginToken
    @Mock private lateinit var neevaActivity: NeevaActivity
    @Mock private lateinit var spaceStore: SpaceStore

    private lateinit var activeTabModel: ActiveTabModel
    private lateinit var browserWrapper: BrowserWrapper
    private lateinit var firstRunModel: FirstRunModel
    private lateinit var neevaUser: NeevaUser
    private lateinit var popupModel: PopupModel
    private lateinit var webLayerModel: WebLayerModel
    private lateinit var neevaActivityViewModel: NeevaActivityViewModel

    private lateinit var dispatchers: Dispatchers

    override fun setUp() {
        super.setUp()

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )

        activeTabModel = mock {
            on { displayedInfoFlow } doReturn MutableStateFlow(ActiveTabModel.DisplayedInfo())
        }

        browserWrapper = mock {
            on { activeTabModel } doReturn activeTabModel
        }

        webLayerModel = mock {
            on { currentBrowser } doReturn browserWrapper
        }

        neevaUser = mock {
            on { loginToken } doReturn loginToken
            on { isSignedOut() } doReturn false
        }

        popupModel = mock {
            on {
                showBottomSheet(any(), any<@Composable (dismiss: () -> Unit) -> Unit>())
            } doAnswer {}
        }

        firstRunModel = mock {
            on { shouldShowPreviewPromptForSignedOutQuery() } doReturn false
        }

        neevaActivityViewModel = NeevaActivityViewModel(
            neevaUser = neevaUser,
            webLayerModel = webLayerModel,
            popupModel = popupModel,
            firstRunModel = firstRunModel,
            spaceStore = spaceStore,
            dispatchers = dispatchers,
            overrideCoroutineScope = coroutineScopeRule.scope
        )
    }

    @Test
    fun signOut() {
        neevaActivityViewModel.signOut()
        coroutineScopeRule.scope.advanceUntilIdle()

        runBlocking {
            verify(spaceStore).deleteAllData()
        }
        verify(loginToken).purgeCachedCookie(any())
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

        verify(popupModel, never()).showSnackbar(any(), eq(null), any(), any(), any())
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

        verify(popupModel, never()).showSnackbar(any(), eq(null), any(), any(), any())
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

        verify(popupModel, never()).showSnackbar(any(), eq(null), any(), any(), any())
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
        verify(popupModel).showSnackbar(any(), eq(null), any(), any(), any())
    }
}
