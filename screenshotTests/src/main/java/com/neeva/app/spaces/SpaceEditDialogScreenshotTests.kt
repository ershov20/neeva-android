package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpaceEditDialogScreenshotTests : BaseScreenshotTest() {
    @Test
    fun spaceEditDialogPreview_Light() = runScreenshotTest {
        SpaceEditDialogPreview_Light()
    }

    @Test
    fun spaceEditDialogPreview_Dark() = runScreenshotTest {
        SpaceEditDialogPreview_Dark()
    }
}
