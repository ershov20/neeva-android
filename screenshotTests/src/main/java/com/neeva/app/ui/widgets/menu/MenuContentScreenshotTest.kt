package com.neeva.app.ui.widgets.menu

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class MenuContentScreenshotTest : BaseScreenshotTest() {
    @Test fun menuContent_Light() = runScreenshotTest { MenuContent_Light() }
    @Test fun menuContent_Dark() = runScreenshotTest { MenuContent_Dark() }
    @Test fun menuContent_OnlyTitle() = runScreenshotTest { MenuContent_OnlyTitle() }
    @Test fun menuContent_OnlyUrl() = runScreenshotTest { MenuContent_OnlyUrl() }

    @Test fun menuContent_OverflowMenu_Light() = runScreenshotTest {
        MenuContent_OverflowMenu_Light()
    }

    @Test fun menuContent_OverflowMenu_Dark() = runScreenshotTest {
        MenuContent_OverflowMenu_Dark()
    }
}
