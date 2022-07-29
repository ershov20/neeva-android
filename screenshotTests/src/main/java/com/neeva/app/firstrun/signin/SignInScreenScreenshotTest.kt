package com.neeva.app.firstrun.signin

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SignInScreenScreenshotTest : BaseScreenshotTest() {
    @Test fun signInScreen_Light_Preview() = runScreenshotTest { SignInScreen_Light_Preview() }
    @Test fun signInScreen_Dark_Preview() = runScreenshotTest { SignInScreen_Dark_Preview() }
}
