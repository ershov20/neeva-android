package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class ShareSpaceUIScreenshotTests : BaseScreenshotTest() {
    @Test
    fun shareSpaceUIPreviewLight() = runScreenshotTest {
        ShareSpaceUIPreviewLight()
    }

    @Test
    fun shareSpaceUIPreviewDark() = runScreenshotTest {
        ShareSpaceUIPreviewDark()
    }
}
