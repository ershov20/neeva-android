package com.neeva.app.cardgrid.tabs

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class TabGridScreenshotTest : BaseScreenshotTest() {
    @Test
    fun tabGridPreview_LightIncognitoArchiving() {
        runScreenshotTest { TabGridPreview_LightIncognitoArchiving() }
    }

    @Test
    fun tabGridPreview_LightRegularArchiving() {
        runScreenshotTest { TabGridPreview_LightRegularArchiving() }
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
    fun tabGridPreview_DarkRegularArchiving() {
        runScreenshotTest { TabGridPreview_DarkRegularArchiving() }
    }

    @Test
    fun tabGridPreview_DarkRegularArchivingWithoutTabs() {
        runScreenshotTest { TabGridPreview_DarkRegularArchivingWithoutTabs() }
    }
}
