package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpaceRowScreenshotTests : BaseScreenshotTest() {
    @Test
    fun spaceRowPreview_AddToSpaces() = runScreenshotTest {
        SpaceRowPreview_AddToSpaces()
    }

    @Test
    fun spaceRowPreview_ZeroQuery() = runScreenshotTest {
        SpaceRowPreview_ZeroQuery()
    }
}
