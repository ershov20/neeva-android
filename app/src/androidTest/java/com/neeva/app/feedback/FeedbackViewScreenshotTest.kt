package com.neeva.app.feedback

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseScreenshotTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackViewScreenshotTest : BaseScreenshotTest() {
    @Test
    fun testFeedbackViewPreview_LightTheme() = runScreenshotTest {
        FeedbackViewPreview_LightTheme()
    }

    @Test
    fun testFeedbackViewPreview_DarkTheme() = runScreenshotTest {
        FeedbackViewPreview_DarkTheme()
    }
}
