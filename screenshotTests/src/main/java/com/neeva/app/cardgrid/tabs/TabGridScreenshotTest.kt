package com.neeva.app.cardgrid.tabs

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class TabGridScreenshotTest : BaseScreenshotTest() {
    @Test
    fun tabGridPreview_LightIncognitoArchiving() {
        runScreenshotTest { TabGridPreview_LightIncognitoArchiving() }
    }

    @Test
    fun tabGridPreview_LightIncognitoLegacy() {
        runScreenshotTest { TabGridPreview_LightIncognitoLegacy() }
    }

    @Test
    fun tabGridPreview_LightRegularArchiving() {
        runScreenshotTest { TabGridPreview_LightRegularArchiving() }
    }

    @Test
    fun tabGridPreview_LightRegularLegacy() {
        runScreenshotTest { TabGridPreview_LightRegularLegacy() }
    }

    @Test
    fun tabGridPreview_LightRegularArchivingWithoutTabs() {
        runScreenshotTest { TabGridPreview_LightRegularArchivingWithoutTabs() }
    }

    @Test
    fun tabGridPreview_DarkIncognitoArchiving() {
        runScreenshotTest { TabGridPreview_DarkIncognitoArchiving() }
    }

    @Test
    fun tabGridPreview_DarkIncognitoLegacy() {
        runScreenshotTest { TabGridPreview_DarkIncognitoLegacy() }
    }

    @Test
    fun tabGridPreview_DarkRegularArchiving() {
        runScreenshotTest { TabGridPreview_DarkRegularArchiving() }
    }

    @Test
    fun tabGridPreview_DarkRegularLegacy() {
        runScreenshotTest { TabGridPreview_DarkRegularLegacy() }
    }

    @Test
    fun tabGridPreview_DarkRegularArchivingWithoutTabs() {
        runScreenshotTest { TabGridPreview_DarkRegularArchivingWithoutTabs() }
    }
}
