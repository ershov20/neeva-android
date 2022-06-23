package com.neeva.app.history

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseScreenshotTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryUIScreenshotTest : BaseScreenshotTest() {
    @Test
    fun historyUI_Preview_Light() = runScreenshotTest {
        HistoryUI_Preview_Light()
    }

    @Test
    fun historyUI_Preview_Dark() = runScreenshotTest {
        HistoryUI_Preview_Dark()
    }
}
