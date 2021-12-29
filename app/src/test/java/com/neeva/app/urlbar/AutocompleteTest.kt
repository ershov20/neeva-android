package com.neeva.app.urlbar

import android.Manifest
import android.net.Uri
import com.neeva.app.suggestions.NavSuggestion
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AutocompleteTest {
    @Test
    fun getUrlToLoad_withAutocompletedSuggestion_usesSuggestion() {
        expectThat(
            getUrlToLoad(
                autocompletedSuggestion = NavSuggestion(
                    url = Uri.parse("https://www.reddit.com"),
                    label = "Primary label",
                    secondaryLabel = "Secondary label"
                ),
                urlBarContents = "https://www.url.bar.contents.com"
            ).toString()
        ).isEqualTo("https://www.reddit.com")
    }

    @Test
    fun getUrlToLoad_withoutAutocompletedSuggestion_usesUrlBarContents() {
        expectThat(
            getUrlToLoad(
                autocompletedSuggestion = null,
                urlBarContents = "https://www.url.bar.contents.com"
            ).toString()
        ).isEqualTo("https://www.url.bar.contents.com")
    }
}