package com.neeva.app.firstrun

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class WelcomeScreenScreenshotTest : BaseScreenshotTest() {
    @Test fun welcomeScreen_Light_Preview() = runScreenshotTest { WelcomeScreen_Light_Preview() }
    @Test fun welcomeScreen_Dark_Preview() = runScreenshotTest { WelcomeScreen_Dark_Preview() }
}
