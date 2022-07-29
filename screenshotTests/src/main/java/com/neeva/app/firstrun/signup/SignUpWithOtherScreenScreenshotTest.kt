package com.neeva.app.firstrun.signup

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SignUpWithOtherScreenScreenshotTest : BaseScreenshotTest() {
    @Test fun signUpOther_Light_Preview() = runScreenshotTest { SignUpOther_Light_Preview() }
    @Test fun signUpOther_Dark_Preview() = runScreenshotTest { SignUpOther_Dark_Preview() }
}
