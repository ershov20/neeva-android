package com.neeva.app.settings

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class LicensesPaneScreenshotTests : BaseScreenshotTest() {
    @Test
    fun licensesPane_Preview_Light() = runScreenshotTest {
        LicensesPane_Preview_Light()
    }

    @Test
    fun licensesPane_Preview_Dark() = runScreenshotTest {
        LicensesPane_Preview_Dark()
    }
}
