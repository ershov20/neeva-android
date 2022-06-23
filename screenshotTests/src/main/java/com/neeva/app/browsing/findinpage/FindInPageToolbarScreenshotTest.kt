package com.neeva.app.browsing.findinpage

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class FindInPageToolbarScreenshotTest : BaseScreenshotTest() {
    @Test
    fun findInPageTest() {
        runScreenshotTest {
            FindInPageToolbarPreview()
        }
    }
}
