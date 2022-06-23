package com.neeva.app.browsing.overflowmenu

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.overflowmenu.OverflowMenuContentsPreviews
import org.junit.Test

class OverflowMenuScreenshotTest : BaseScreenshotTest() {
    @Test
    fun darkTheme_ForwardEnabled_UpdateAvailableVisible_DesktopSite_Test() {
        runScreenshotTest {
            OverflowMenuContentsPreviews()
                .PreviewDark_ForwardEnabled_UpdateAvailableVisible_DesktopSite()
        }
    }

    @Test
    fun lightTheme_ForwardEnabled_UpdateAvailableVisible_DesktopSite_Test() {
        runScreenshotTest {
            OverflowMenuContentsPreviews()
                .PreviewLight_ForwardEnabled_UpdateAvailableVisible_DesktopSite()
        }
    }

    @Test
    fun lightTheme_ForwardDisabled_DesktopSite_Test() {
        runScreenshotTest {
            OverflowMenuContentsPreviews().PreviewLight_ForwardDisabled_DesktopSite()
        }
    }

    @Test
    fun lightTheme_ForwardEnabled_MobileSite_Test() {
        runScreenshotTest {
            OverflowMenuContentsPreviews().PreviewLight_ForwardEnabled_MobileSite()
        }
    }
}
