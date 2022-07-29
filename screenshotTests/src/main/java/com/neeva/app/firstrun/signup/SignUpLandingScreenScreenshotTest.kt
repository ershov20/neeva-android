package com.neeva.app.firstrun.signup

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SignUpLandingScreenScreenshotTest : BaseScreenshotTest() {
    @Test fun signUpLanding_Light_Preview() = runScreenshotTest { SignUpLanding_Light_Preview() }
    @Test fun signUpLanding_Dark_Preview() = runScreenshotTest { SignUpLanding_Dark_Preview() }
}
