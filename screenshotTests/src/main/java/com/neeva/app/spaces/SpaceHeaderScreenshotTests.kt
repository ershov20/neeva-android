package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SpaceHeaderScreenshotTests : BaseScreenshotTest() {
    @Test
    fun spaceHeaderStatsPreview_IsOwner() = runScreenshotTest {
        SpaceHeaderStatsPreview_IsOwner()
    }

    @Test
    fun spaceHeaderStatsPreview_NotOwner() = runScreenshotTest {
        SpaceHeaderStatsPreview_NotOwner()
    }

    @Test
    fun spaceHeaderPreviewLight_NotOwner() = runScreenshotTest {
        SpaceHeaderPreviewLight_NotOwner()
    }

    @Test
    fun spaceHeaderPreviewLight_IsOwner() = runScreenshotTest {
        SpaceHeaderPreviewLight_IsOwner()
    }

    @Test
    fun spaceHeaderPreviewDark_NotOwner() = runScreenshotTest {
        SpaceHeaderPreviewDark_NotOwner()
    }

    @Test
    fun spaceHeaderPreviewDark_IsOwner() = runScreenshotTest {
        SpaceHeaderPreviewDark_IsOwner()
    }
}
