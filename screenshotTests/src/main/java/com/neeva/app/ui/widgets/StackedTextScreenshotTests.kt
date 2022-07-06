package com.neeva.app.ui.widgets

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class StackedTextScreenshotTests : BaseScreenshotTest() {
    @Test
    fun stackedTextPreview() = runScreenshotTest { StackedTextPreview() }
}
