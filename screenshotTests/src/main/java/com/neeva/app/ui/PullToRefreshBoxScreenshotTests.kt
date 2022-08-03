package com.neeva.app.ui

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class PullToRefreshBoxScreenshotTests : BaseScreenshotTest() {
    @Test
    fun pullToRefreshBox_Preview() = runScreenshotTest { PullToRefreshBox_Preview() }
}
