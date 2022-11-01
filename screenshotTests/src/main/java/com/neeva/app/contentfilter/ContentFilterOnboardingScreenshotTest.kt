package com.neeva.app.contentfilter

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.contentfilter.ui.popover.ContentFilterAdBlockOnboardingPopoverContentPreview_Dark
import com.neeva.app.contentfilter.ui.popover.ContentFilterAdBlockOnboardingPopoverContentPreview_Light_Full
import com.neeva.app.contentfilter.ui.popover.ContentFilterCookieNoticeOnboardingPopoverContentPreview_Dark
import com.neeva.app.contentfilter.ui.popover.ContentFilterCookieNoticeOnboardingPopoverContentPreview_Light_Full
import com.neeva.app.contentfilter.ui.popover.ContentFilterDisableOnboardingContentPreview_Light_Partial
import org.junit.Test

class ContentFilterOnboardingScreenshotTest : BaseScreenshotTest() {
    @Test
    fun contentFilterDisableOnboardinContentPreview_Light_Partial() = runScreenshotTest {
        ContentFilterDisableOnboardingContentPreview_Light_Partial()
    }

    @Test
    fun contentFilterCookieNoticeOnboardingPopoverContentPreview_Light_Full() = runScreenshotTest {
        ContentFilterCookieNoticeOnboardingPopoverContentPreview_Light_Full()
    }

    @Test
    fun contentFilterCookieNoticeOnboardingPopoverContentPreview_Dark() = runScreenshotTest {
        ContentFilterCookieNoticeOnboardingPopoverContentPreview_Dark()
    }

    @Test
    fun contentFilterAdBlockOnboardingPopoverContentPreview_Light_Full() = runScreenshotTest {
        ContentFilterAdBlockOnboardingPopoverContentPreview_Light_Full()
    }

    @Test
    fun contentFilterAdBlockOnboardingPopoverContentPreview_Dark() = runScreenshotTest {
        ContentFilterAdBlockOnboardingPopoverContentPreview_Dark()
    }
}
