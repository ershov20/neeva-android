package com.neeva.app.browsing.toolbar

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class BrowserToolbarScreenshotTest : BaseScreenshotTest() {
    @Test
    fun toolbarPreview_Blank_Portrait() = runScreenshotTest {
        ToolbarPreview_Blank_Portrait()
    }

    @Test
    fun toolbarPreview_Blank_Landscape() = runScreenshotTest {
        ToolbarPreview_Blank_Landscape()
    }

    @Test
    fun toolbarPreview_Blank_NeevaScopeEnabled_Landscape() = runScreenshotTest {
        ToolbarPreview_Blank_NeevaScopeEnabled_Landscape()
    }

    @Test
    fun toolbarPreview_Focus_Portrait() = runScreenshotTest {
        ToolbarPreview_Focus_Portrait()
    }

    @Test
    fun toolbarPreview_Focus_Landscape() = runScreenshotTest {
        ToolbarPreview_Focus_Landscape()
    }

    @Test
    fun toolbarPreview_Typing_Portrait() = runScreenshotTest {
        ToolbarPreview_Typing_Portrait()
    }

    @Test
    fun toolbarPreview_Typing_Landscape() = runScreenshotTest {
        ToolbarPreview_Typing_Landscape()
    }

    @Test
    fun toolbarPreview_Search_Portrait() = runScreenshotTest {
        ToolbarPreview_Search_Portrait()
    }

    @Test
    fun toolbarPreview_Search_Landscape() = runScreenshotTest {
        ToolbarPreview_Search_Landscape()
    }

    @Test
    fun toolbarPreview_Loading_Portrait() = runScreenshotTest {
        ToolbarPreview_Loading_Portrait()
    }

    @Test
    fun toolbarPreview_Loading_Landscape() = runScreenshotTest {
        ToolbarPreview_Loading_Landscape()
    }
}
