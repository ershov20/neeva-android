package com.neeva.app.ui

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class NeevaSwitchScreenshotTests : BaseScreenshotTest() {
    @Test
    fun neevaSwitchPreviewEnabled() = runScreenshotTest { NeevaSwitchPreviewEnabled() }

    @Test
    fun neevaSwitchPreviewDisabled() = runScreenshotTest { NeevaSwitchPreviewDisabled() }

    @Test
    fun neevaSwitchPreviewEnabled_NotChecked() = runScreenshotTest { NeevaSwitchPreviewEnabled_NotChecked() }

    @Test
    fun neevaSwitchPreviewDisabled_NotChecked() = runScreenshotTest { NeevaSwitchPreviewDisabled_NotChecked() }
}
