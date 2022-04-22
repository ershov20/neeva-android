package com.neeva.app.firstrun.widgets.texts

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class AcknowledgementTextScreenshotTest : BaseScreenshotTest() {
    @Test
    fun acknowledgementTextPreview() = runScreenshotTest { AcknowledgementTextPreview() }
}
