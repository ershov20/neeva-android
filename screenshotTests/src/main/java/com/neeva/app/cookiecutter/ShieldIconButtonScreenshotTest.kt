package com.neeva.app.cookiecutter

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.cookiecutter.ui.icon.IncognitoIconButtonPreview
import com.neeva.app.cookiecutter.ui.icon.ShieldIconButtonPreview
import org.junit.Test

class ShieldIconButtonScreenshotTest : BaseScreenshotTest() {
    @Test
    fun shieldIconButtonPreview() = runScreenshotTest { ShieldIconButtonPreview() }

    @Test
    fun incognitoIconButtonPreview() = runScreenshotTest { IncognitoIconButtonPreview() }
}
