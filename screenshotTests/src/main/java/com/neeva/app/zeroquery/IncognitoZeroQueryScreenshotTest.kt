package com.neeva.app.zeroquery

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
