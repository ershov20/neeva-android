package com.neeva.app.browsing

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class UriFuzzyMatchDataTest : BaseTest() {
    @Test
    fun normalizedForFuzzyMatching() {
        expectThat(UriFuzzyMatchData.create(Uri.parse("http://www.reddit.com")))
            .isEqualTo(UriFuzzyMatchData("reddit.com", "", null))
        expectThat(UriFuzzyMatchData.create(Uri.parse("https://m.facebook.com")))
            .isEqualTo(UriFuzzyMatchData("facebook.com", "", null))
        expectThat(UriFuzzyMatchData.create(Uri.parse("http://en.m.wikipedia.org")))
            .isEqualTo(UriFuzzyMatchData("en.wikipedia.org", "", null))
        expectThat(UriFuzzyMatchData.create(Uri.parse("https://de.m.wikipedia.org")))
            .isEqualTo(UriFuzzyMatchData("de.wikipedia.org", "", null))

        expectThat(UriFuzzyMatchData.create(Uri.parse("https://www.hostwithport.com:8000")))
            .isEqualTo(UriFuzzyMatchData("hostwithport.com:8000", "", null))

        expectThat(
            UriFuzzyMatchData.create(
                Uri.parse("https://www.example.com/path/trailing/?hl=en-US&gl=US&ceid=US:en")
            )
        ).isEqualTo(
            UriFuzzyMatchData("example.com", "/path/trailing", "hl=en-US&gl=US&ceid=US:en")
        )
    }

    @Test
    fun fuzzyMatch() {
        // Same URL check
        runFuzzyMatchTest("https://www.reddit.com/", "https://www.reddit.com/", true)

        runFuzzyMatchTest("https://www.reddit.com/r/all", "https://reddit.com/r/all", true)
        runFuzzyMatchTest("https://www.reddit.com/r/all", "http://reddit.com/r/all", true)

        // Wikipedia strings.
        runFuzzyMatchTest(
            "https://en.wikipedia.org/wiki/Prime_number",
            "https://en.m.wikipedia.org/wiki/Prime_number",
            true
        )

        runFuzzyMatchTest(
            "https://de.wikipedia.org/wiki/Prime_number",
            "https://en.m.wikipedia.org/wiki/Prime_number",
            false
        )

        runFuzzyMatchTest(
            "https://en.wikipedia.org/wiki/Prime_number",
            "https://de.wikipedia.org/wiki/Prime_number",
            false
        )

        runFuzzyMatchTest(
            "https://www.example.com/path/trailing/?hl=en-US&gl=US&ceid=US:en",
            "http://example.com/path/trailing?hl=en-US&gl=US&ceid=US:en",
            true
        )

        runFuzzyMatchTest(
            "https://www.example.com/path/trailing/?hl=en-US&gl=US&ceid=US:en",
            "http://example.com/path/trailing?hl=en-US&gl=US&ceid=US:de",
            false
        )
    }

    private fun runFuzzyMatchTest(first: String, second: String, expectedOutcome: Boolean) {
        val firstUri = UriFuzzyMatchData.create(Uri.parse(first))
        val secondUri = UriFuzzyMatchData.create(Uri.parse(second))
        expectThat(firstUri == secondUri).isEqualTo(expectedOutcome)
    }
}
