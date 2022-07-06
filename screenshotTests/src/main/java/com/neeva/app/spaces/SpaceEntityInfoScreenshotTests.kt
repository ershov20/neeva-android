package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpaceEntityInfoScreenshotTests : BaseScreenshotTest() {
    @Test
    fun spaceEntityRecipeInfoPreview() = runScreenshotTest {
        SpaceEntityRecipeInfoPreview()
    }

    @Test
    fun spaceEntityProductInfoPreview() = runScreenshotTest {
        SpaceEntityProductInfoPreview()
    }
}
