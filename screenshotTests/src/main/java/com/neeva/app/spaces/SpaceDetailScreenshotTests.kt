package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpaceDetailScreenshotTests : BaseScreenshotTest() {
    @Test
    fun spaceDetailToolbarPreview() = runScreenshotTest {
        SpaceDetailToolbarPreview()
    }

    @Test
    fun spaceDetailToolbarPreviewNoOwner() = runScreenshotTest {
        SpaceDetailToolbarPreviewNoOwner()
    }

    @Test
    fun spaceDetailToolbarPreviewNoTitle() = runScreenshotTest {
        SpaceDetailToolbarPreviewNoTitle()
    }
}
