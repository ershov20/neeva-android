package com.neeva.app.zeroQuery

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class IncognitoZeroQueryScreenshotTest : BaseScreenshotTest() {
    @Test
    fun incognitoZeroQueryPreview_Light() = runScreenshotTest {
        IncognitoZeroQueryPreview_Light()
    }

    @Test
    fun incognitoZeroQueryPreview_Dark() = runScreenshotTest {
        IncognitoZeroQueryPreview_Dark()
    }
}
