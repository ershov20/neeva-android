package com.neeva.app

import org.junit.Rule

/** Base class for tests that need to be served with fake webpages. */
abstract class BaseBrowserTest {
    @get:Rule
    val webpageServingRule = WebpageServingRule()
}
