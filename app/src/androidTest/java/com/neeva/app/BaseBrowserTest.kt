package com.neeva.app

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule

/** Base class for tests that need to be served with fake webpages. */
abstract class BaseBrowserTest : BaseHiltTest() {
    @get:Rule
    val webpageServingRule = WebpageServingRule()

    override fun setUp() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                InstrumentationRegistry.getInstrumentation().targetContext,
                "Starting ${this::class.simpleName}",
                Toast.LENGTH_SHORT
            ).show()
        }
        super.setUp()
    }
}
