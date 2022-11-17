package com.neeva.app.neevascope

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class NeevaScopeScreenshotTests : BaseScreenshotTest() {
    @Test
    fun redditDiscussionRow_withComments_Preview() = runScreenshotTest {
        RedditDiscussionRow_withComments_Preview()
    }

    @Test
    fun webResultRow_Preview() = runScreenshotTest {
        WebResultRow_Preview()
    }

    @Test
    fun recipeHeader_Preview() = runScreenshotTest {
        RecipeHeader_Preview()
    }

    @Test
    fun supportSection_Preview() = runScreenshotTest {
        SupportSection_Preview()
    }

    @Test
    fun neevaScopeNoResultScreen_Preview() = runScreenshotTest {
        NeevaScopeNoResultScreen_Preview()
    }

    @Test
    fun neevaScopeLoadingScreen_Preview() = runScreenshotTest {
        NeevaScopeLoadingScreen_Preview()
    }
}
