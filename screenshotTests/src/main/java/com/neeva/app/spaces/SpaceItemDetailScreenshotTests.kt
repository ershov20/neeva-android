package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpaceItemDetailScreenshotTests : BaseScreenshotTest() {
    @Test
    fun spaceItemPreviewLight() = runScreenshotTest {
        SpaceItemPreviewLight()
    }

    @Test
    fun spaceItemPreviewDark() = runScreenshotTest {
        SpaceItemPreviewDark()
    }

    @Test
    fun spaceSectionHeaderPreviewLight() = runScreenshotTest {
        SpaceSectionHeaderPreviewLight()
    }

    @Test
    fun spaceSectionHeaderPreviewDark() = runScreenshotTest {
        SpaceSectionHeaderPreviewDark()
    }
}
