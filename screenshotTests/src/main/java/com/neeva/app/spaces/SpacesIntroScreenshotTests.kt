package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpacesIntroScreenshotTests : BaseScreenshotTest() {
    @Test
    fun spacesIntroPreviewLight() = runScreenshotTest {
        SpacesIntroPreviewLight()
    }

    @Test
    fun spacesIntroPreviewDark() = runScreenshotTest {
        SpacesIntroPreviewDark()
    }
}
