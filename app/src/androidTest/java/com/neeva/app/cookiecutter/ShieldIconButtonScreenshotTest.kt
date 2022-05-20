package com.neeva.app.cookiecutter

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.cookiecutter.ui.IncognitoIconButtonPreview
import com.neeva.app.cookiecutter.ui.ShieldIconButtonPreview
import org.junit.Test

class ShieldIconButtonScreenshotTest : BaseScreenshotTest() {
    @Test
    fun shieldIconButtonPreview() = runScreenshotTest { ShieldIconButtonPreview() }

    @Test
    fun incognitoIconButtonPreview() = runScreenshotTest { IncognitoIconButtonPreview() }
}
