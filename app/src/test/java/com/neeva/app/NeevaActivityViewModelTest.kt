package com.neeva.app

import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class NeevaActivityViewModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var neevaUser: NeevaUser
    @Mock private lateinit var spaceStore: SpaceStore
    @Mock private lateinit var webLayerModel: WebLayerModel

    private lateinit var neevaActivityViewModel: NeevaActivityViewModel

    private lateinit var dispatchers: Dispatchers

    override fun setUp() {
        super.setUp()

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )

        neevaActivityViewModel = NeevaActivityViewModel(
            pendingLaunchIntent = null,
            neevaUser = neevaUser,
            webLayerModel = webLayerModel,
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
}
