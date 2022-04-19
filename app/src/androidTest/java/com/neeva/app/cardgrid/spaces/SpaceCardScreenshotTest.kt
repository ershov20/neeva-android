package com.neeva.app.cardgrid.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpaceCardScreenshotTest : BaseScreenshotTest() {
    @Test
    fun longString() {
        runScreenshotTest {
            SpaceCardPreview_LongString()
        }
    }

    @Test
    fun shortTitleSelected() {
        runScreenshotTest {
            TabCardPreview_ShortTitleSelected()
        }
    }
}
