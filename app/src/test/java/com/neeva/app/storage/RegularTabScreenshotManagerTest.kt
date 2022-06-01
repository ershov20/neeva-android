package com.neeva.app.storage

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.chromium.weblayer.CaptureScreenShotCallback
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
class RegularTabScreenshotManagerTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var filesDir: File
    private lateinit var tabScreenshotManager: TabScreenshotManager
    private lateinit var navigationController: NavigationController

    override fun setUp() {
        super.setUp()

        val context: Context = ApplicationProvider.getApplicationContext()
        filesDir = context.filesDir

        navigationController = mock {
            on { getNavigationListSize() } doReturn 1
        }

        tabScreenshotManager = RegularTabScreenshotManager(
            filesDir = filesDir,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = Dispatchers(
                main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
                io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler)
            )
        )
    }

    override fun tearDown() {
        super.tearDown()
        filesDir.deleteRecursively()
    }

    @Test
    fun captureAndSaveScreenshot_onCaptureSuccess_writesRestorableFile() {
        val tab: Tab = mock {
            on { isDestroyed } doReturn false
            on { guid } doReturn "uuid"
            on { getNavigationController() } doReturn navigationController
        }
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        expectThat(tabScreenshotManager.getTabScreenshotFile(tab).exists()).isFalse()

        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
        verify(tab, times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())

        // Say that nothing went wrong when trying to save the screenshot.
        val bitmap = Bitmap.createBitmap(32, 64, Bitmap.Config.ARGB_8888)
        expectThat(bitmap.width).isEqualTo(32)
        expectThat(bitmap.height).isEqualTo(64)

        // Fire the callback and wait for everything to completely settle.
        captureCallbackCaptor.firstValue.onScreenShotCaptured(bitmap, 0)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(onCompleted, times(1)).invoke()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tab).exists()).isTrue()

        // Restore the bitmap and confirm that the size is correct.  It'd be nice to actually
        // confirm that the pixel values match, but Robolectric's ShadowBitmaps don't seem to play
        // well with the Bitmap color accessors.
        val restoredBitmap = tabScreenshotManager.restoreScreenshot(tab.guid)
        expectThat(restoredBitmap!!.width).isEqualTo(bitmap.width)
        expectThat(restoredBitmap.height).isEqualTo(bitmap.height)
    }

    @Test
    fun captureAndSaveScreenshot_onCaptureSuccess_firesOnCompletedLambda() {
        val tab: Tab = mock {
            on { isDestroyed } doReturn false
            on { guid } doReturn "uuid"
            on { getNavigationController() } doReturn navigationController
        }
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
        verify(tab, times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())

        // Say that nothing went wrong when trying to save the screenshot.
        captureCallbackCaptor.firstValue.onScreenShotCaptured(null, 0)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(onCompleted, times(1)).invoke()
    }

    @Test
    fun captureAndSaveScreenshot_onTimeout_firesOnCompletedLambda() {
        val tab: Tab = mock {
            on { isDestroyed } doReturn false
            on { guid } doReturn "uuid"
            on { getNavigationController() } doReturn navigationController
        }
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
        verify(tab, times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())
        verify(onCompleted, never()).invoke()

        // Advance the clock until even the timeout fires.  onCompleted should still fire even
        // though we never fire the callback.
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(onCompleted, times(1)).invoke()
    }

    @Test
    fun captureAndSaveScreenshot_onCaptureError_stillFiresOnCompletedLambda() {
        val tab: Tab = mock {
            on { isDestroyed } doReturn false
            on { guid } doReturn "uuid"
            on { getNavigationController() } doReturn navigationController
        }
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
        verify(tab, times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())

        // Say that something went wrong when trying to save the screenshot.
        captureCallbackCaptor.firstValue.onScreenShotCaptured(null, 10)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(onCompleted, times(1)).invoke()
    }

    @Test
    fun captureAndSaveScreenshot_withNoNavigations_stillFiresOnCompletedLambda() {
        whenever(navigationController.getNavigationListSize()).thenReturn(0)
        val tab: Tab = mock {
            on { isDestroyed } doReturn false
            on { guid } doReturn "uuid"
            on { getNavigationController() } doReturn navigationController
        }
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        // Because the tab has no navigations, it shouldn't fire the capture callback.  It should,
        // however, still try to fire the onCompleted callback.
        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
        verify(tab, never()).captureScreenShot(any(), captureCallbackCaptor.capture())
        verify(onCompleted, times(1)).invoke()

        // Because we pass in null, it shouldn't fire the capture callback.
        tabScreenshotManager.captureAndSaveScreenshot(null, onCompleted)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(tab, never()).captureScreenShot(any(), captureCallbackCaptor.capture())
        verify(onCompleted, times(2)).invoke()
    }

    @Test
    fun captureAndSaveScreenshot_withUnusableTab_stillFiresOnCompletedLambda() {
        val tab: Tab = mock {
            on { isDestroyed } doReturn true
            on { guid } doReturn "uuid"
            on { getNavigationController() } doReturn navigationController
        }
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        // Because the tab is destroyed, it shouldn't fire the capture callback.  It should,
        // however, still try to fire the onCompleted callback.
        tabScreenshotManager.captureAndSaveScreenshot(tab, onCompleted)
        verify(tab, never()).captureScreenShot(any(), captureCallbackCaptor.capture())
        verify(onCompleted, times(1)).invoke()

        // Because we pass in null, it shouldn't fire the capture callback.
        tabScreenshotManager.captureAndSaveScreenshot(null, onCompleted)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(tab, never()).captureScreenShot(any(), captureCallbackCaptor.capture())
        verify(onCompleted, times(2)).invoke()
    }

    @Test
    fun deleteScreenshot() {
        val tabs: List<Tab> = listOf(
            mock {
                on { isDestroyed } doReturn false
                on { guid } doReturn "tab 1"
                on { getNavigationController() } doReturn navigationController
            },
            mock {
                on { isDestroyed } doReturn false
                on { guid } doReturn "tab 2"
                on { getNavigationController() } doReturn navigationController
            },
            mock {
                on { isDestroyed } doReturn false
                on { guid } doReturn "tab 3"
                on { getNavigationController() } doReturn navigationController
            }
        )
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[0]).exists()).isFalse()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[1]).exists()).isFalse()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[2]).exists()).isFalse()

        // Save each of the Tab's bitmaps out.
        val bitmap = Bitmap.createBitmap(32, 64, Bitmap.Config.ARGB_8888)
        expectThat(bitmap.width).isEqualTo(32)
        expectThat(bitmap.height).isEqualTo(64)

        tabScreenshotManager.captureAndSaveScreenshot(tabs[0], onCompleted)
        verify(tabs[0], times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())
        captureCallbackCaptor.lastValue.onScreenShotCaptured(bitmap, 0)
        coroutineScopeRule.scope.advanceUntilIdle()

        tabScreenshotManager.captureAndSaveScreenshot(tabs[1], onCompleted)
        verify(tabs[1], times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())
        captureCallbackCaptor.lastValue.onScreenShotCaptured(bitmap, 0)
        coroutineScopeRule.scope.advanceUntilIdle()

        tabScreenshotManager.captureAndSaveScreenshot(tabs[2], onCompleted)
        verify(tabs[2], times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())
        captureCallbackCaptor.lastValue.onScreenShotCaptured(bitmap, 0)
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[0]).exists()).isTrue()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[1]).exists()).isTrue()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[2]).exists()).isTrue()

        // Delete one of the tab screenshots.
        tabScreenshotManager.deleteScreenshot(tabs[1].guid)
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[0]).exists()).isTrue()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[1]).exists()).isFalse()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[2]).exists()).isTrue()
    }

    @Test
    fun cleanCacheDirectory_removesDeadTabScreenshots() {
        val tabs: List<Tab> = listOf(
            mock {
                on { isDestroyed } doReturn false
                on { guid } doReturn "tab 1"
                on { getNavigationController() } doReturn navigationController
            },
            mock {
                on { isDestroyed } doReturn false
                on { guid } doReturn "tab 2"
                on { getNavigationController() } doReturn navigationController
            },
            mock {
                on { isDestroyed } doReturn false
                on { guid } doReturn "tab 3"
                on { getNavigationController() } doReturn navigationController
            }
        )
        val onCompleted: () -> Unit = mock()
        val captureCallbackCaptor = argumentCaptor<CaptureScreenShotCallback>()

        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[0]).exists()).isFalse()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[1]).exists()).isFalse()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[2]).exists()).isFalse()

        // Save each of the Tab's bitmaps out.
        val bitmap = Bitmap.createBitmap(32, 64, Bitmap.Config.ARGB_8888)
        expectThat(bitmap.width).isEqualTo(32)
        expectThat(bitmap.height).isEqualTo(64)

        tabScreenshotManager.captureAndSaveScreenshot(tabs[0], onCompleted)
        verify(tabs[0], times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())
        captureCallbackCaptor.lastValue.onScreenShotCaptured(bitmap, 0)
        coroutineScopeRule.scope.advanceUntilIdle()

        tabScreenshotManager.captureAndSaveScreenshot(tabs[1], onCompleted)
        verify(tabs[1], times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())
        captureCallbackCaptor.lastValue.onScreenShotCaptured(bitmap, 0)
        coroutineScopeRule.scope.advanceUntilIdle()

        tabScreenshotManager.captureAndSaveScreenshot(tabs[2], onCompleted)
        verify(tabs[2], times(1)).captureScreenShot(any(), captureCallbackCaptor.capture())
        captureCallbackCaptor.lastValue.onScreenShotCaptured(bitmap, 0)
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[0]).exists()).isTrue()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[1]).exists()).isTrue()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[2]).exists()).isTrue()

        // Say that only one of the tabs is still alive.  Two of the screenshots should get purged.
        tabScreenshotManager.cleanCacheDirectory(listOf(tabs[1].guid))
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[0]).exists()).isFalse()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[1]).exists()).isTrue()
        expectThat(tabScreenshotManager.getTabScreenshotFile(tabs[2]).exists()).isFalse()
    }
}
