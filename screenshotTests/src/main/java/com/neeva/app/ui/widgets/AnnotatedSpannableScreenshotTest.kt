package com.neeva.app.ui.widgets

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class AnnotatedSpannableScreenshotTest : BaseScreenshotTest() {
    @Test
    fun annotatedSpannablePreview() = runScreenshotTest { AnnotatedSpannablePreview() }
}
