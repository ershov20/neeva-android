package com.neeva.app.urlbar

import android.net.Uri
import com.neeva.app.suggestions.NavSuggestion
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

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

    @Test
    fun getAutocompleteText_withNoSubdomain() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://www.reddit.com/r/android"),
            label = "Primary label",
            secondaryLabel = "https://www.reddit.com/r/android"
        )

        expectThat(getAutocompleteText(autocompleteSuggestion, "http"))
            .isEqualTo("https://www.reddit.com/r/android")
        expectThat(getAutocompleteText(autocompleteSuggestion, "redd"))
            .isEqualTo("reddit.com/r/android")
        expectThat(getAutocompleteText(autocompleteSuggestion, "mismatch")).isNull()
        expectThat(getAutocompleteText(autocompleteSuggestion, "com")).isNull()
    }

    @Test
    fun getAutocompleteText_withSubdomain() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://news.google.com"),
            label = "Primary label",
            secondaryLabel = "https://news.google.com"
        )

        expectThat(getAutocompleteText(autocompleteSuggestion, "http"))
            .isEqualTo("https://news.google.com")
        expectThat(getAutocompleteText(autocompleteSuggestion, "news"))
            .isEqualTo("news.google.com")
        expectThat(getAutocompleteText(autocompleteSuggestion, "google")).isNull()
        expectThat(getAutocompleteText(autocompleteSuggestion, "mismatch")).isNull()
        expectThat(getAutocompleteText(autocompleteSuggestion, "com")).isNull()
    }
}
