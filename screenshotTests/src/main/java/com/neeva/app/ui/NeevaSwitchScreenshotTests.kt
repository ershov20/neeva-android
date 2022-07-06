package com.neeva.app.ui

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class NeevaSwitchScreenshotTests : BaseScreenshotTest() {
    @Test
    fun neevaSwitchPreviewEnabled() = runScreenshotTest { NeevaSwitchPreviewEnabled() }

    @Test
    fun neevaSwitchPreviewDisabled() = runScreenshotTest { NeevaSwitchPreviewDisabled() }
}
