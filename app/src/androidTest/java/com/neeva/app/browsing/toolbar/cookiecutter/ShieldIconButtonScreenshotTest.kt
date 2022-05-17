package com.neeva.app.browsing.toolbar.cookiecutter

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.browsing.urlbar.trackingprotection.IncognitoIconButtonPreview
import com.neeva.app.browsing.urlbar.trackingprotection.ShieldIconButtonPreview
import org.junit.Test

class ShieldIconButtonScreenshotTest : BaseScreenshotTest() {
    @Test
    fun shieldIconButtonPreview() = runScreenshotTest { ShieldIconButtonPreview() }

    @Test
    fun incognitoIconButtonPreview() = runScreenshotTest { IncognitoIconButtonPreview() }
}
