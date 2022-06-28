package com.neeva.app.ui.widgets

import android.net.Uri
import com.neeva.app.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class UriDisplayViewTest : BaseTest() {
    @Test
    fun parseURLPath() {
        parseURLPath(Uri.parse("https://www.neeva.com/alpha/123/path/to/x/")).apply {
            expectThat(first).isEqualTo("neeva.com")
            expectThat(second).containsExactly("alpha", "123", "path", "to", "x")
        }

        parseURLPath(Uri.parse("https://127.0.0.1:8000/alpha/123/path/to/x/")).apply {
            expectThat(first).isEqualTo("127.0.0.1:8000")
            expectThat(second).containsExactly("alpha", "123", "path", "to", "x")
        }

        // "#" is not a valid path character, which makes Uri.parse discard everything after it.
        parseURLPath(Uri.parse("https://www.neeva.com/#/path/to/x/")).apply {
            expectThat(first).isEqualTo("neeva.com")
            expectThat(second).isEmpty()
        }
    }
}
