package com.neeva.app.cardgrid

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SegmentedPickerScreenshotTest : BaseScreenshotTest() {
    @Test
    fun runIncognitoTest() {
        runScreenshotTest {
            SegmentedPickerPreview_Incognito()
        }
    }

    @Test
    fun runRegularTest() {
        runScreenshotTest {
            SegmentedPickerPreview_Regular()
        }
    }

    @Test
    fun runSpacesTest() {
        runScreenshotTest {
            SegmentedPickerPreview_Spaces()
        }
    }
}
