package com.neeva.app.cardgrid.tabs

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class TabGridEmptyStateScreenshotTest : BaseScreenshotTest() {
    @Test
    fun tabGridEmptyStatePreview_Light() {
        runScreenshotTest { TabGridEmptyStatePreview_Light() }
    }

    @Test
    fun tabGridEmptyStatePreview_Dark() {
        runScreenshotTest { TabGridEmptyStatePreview_Dark() }
    }
}
