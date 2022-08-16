package com.neeva.app.browsing.toolbar

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class BrowserBottomToolbarScreenshotTest : BaseScreenshotTest() {
    @Test
    fun bottomToolbarPreview_Regular() = runScreenshotTest {
        BottomToolbarPreview_Regular()
    }

    @Test
    fun bottomToolbarPreview_NeevaScopeEnabled() = runScreenshotTest {
        BottomToolbarPreview_NeevaScopeEnabled()
    }

    @Test
    fun bottomToolbarPreview_CanGoBackward() = runScreenshotTest {
        BottomToolbarPreview_CanGoBackward()
    }

    @Test
    fun bottomToolbarPreview_SpaceStoreHasUrl() = runScreenshotTest {
        BottomToolbarPreview_SpaceStoreHasUrl()
    }
}
