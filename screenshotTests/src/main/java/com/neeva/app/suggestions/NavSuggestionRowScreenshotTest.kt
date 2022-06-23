package com.neeva.app.suggestions

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class NavSuggestionRowScreenshotTest : BaseScreenshotTest() {
    @Test
    fun longLabelsTest() {
        runScreenshotTest {
            NavSuggestionRowPreview_LongLabels()
        }
    }

    @Test
    fun shortLabelsTest() {
        runScreenshotTest {
            NavSuggestionRowPreview_ShortLabels()
        }
    }
}
