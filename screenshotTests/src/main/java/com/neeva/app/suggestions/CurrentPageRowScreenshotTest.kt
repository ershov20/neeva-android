package com.neeva.app.suggestions

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class CurrentPageRowScreenshotTest : BaseScreenshotTest() {
    @Test
    fun currentPageRowPreview() = runScreenshotTest { CurrentPageRowPreview() }
}
