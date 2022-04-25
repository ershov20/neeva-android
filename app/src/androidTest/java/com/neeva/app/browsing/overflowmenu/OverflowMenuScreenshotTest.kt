package com.neeva.app.browsing.overflowmenu

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.neeva_menu.OverflowMenuContentsPreviews
import org.junit.Test

class OverflowMenuScreenshotTest : BaseScreenshotTest() {
    @Test
    fun darkTheme_ForwardEnabled_UpdateAvailableVisible_DesktopSite_Test() {
        runScreenshotTest {
            val previewClass = OverflowMenuContentsPreviews()
            previewClass.DefaultPreview(
                params = OverflowMenuContentsPreviews.Params(
                    darkTheme = true,
                    isForwardEnabled = true,
                    isUpdateAvailableVisible = true,
                    desktopUserAgentEnabled = true,
                    hideButtons = false
                )
            )
        }
    }

    @Test
    fun lightTheme_ForwardEnabled_UpdateAvailableVisible_DesktopSite_Test() {
        runScreenshotTest {
            val previewClass = OverflowMenuContentsPreviews()
            previewClass.DefaultPreview(
                params = OverflowMenuContentsPreviews.Params(
                    darkTheme = false,
                    isForwardEnabled = true,
                    isUpdateAvailableVisible = true,
                    desktopUserAgentEnabled = true,
                    hideButtons = false
                )
            )
        }
    }

    @Test
    fun lightTheme_ForwardDisabled_DesktopSite_Test() {
        runScreenshotTest {
            val previewClass = OverflowMenuContentsPreviews()
            previewClass.DefaultPreview(
                params = OverflowMenuContentsPreviews.Params(
                    darkTheme = false,
                    isForwardEnabled = false,
                    isUpdateAvailableVisible = false,
                    desktopUserAgentEnabled = true,
                    hideButtons = false
                )
            )
        }
    }

    @Test
    fun lightTheme_ForwardEnabled_MobileSite_Test() {
        runScreenshotTest {
            val previewClass = OverflowMenuContentsPreviews()
            previewClass.DefaultPreview(
                params = OverflowMenuContentsPreviews.Params(
                    darkTheme = false,
                    isForwardEnabled = true,
                    isUpdateAvailableVisible = false,
                    desktopUserAgentEnabled = false,
                    hideButtons = false
                )
            )
        }
    }

    @Test
    fun lightTheme_ForwardEnabled_MobileSite_HideButtons_Test() {
        runScreenshotTest {
            val previewClass = OverflowMenuContentsPreviews()
            previewClass.DefaultPreview(
                params = OverflowMenuContentsPreviews.Params(
                    darkTheme = false,
                    isForwardEnabled = true,
                    isUpdateAvailableVisible = false,
                    desktopUserAgentEnabled = false,
                    hideButtons = true
                )
            )
        }
    }
}
