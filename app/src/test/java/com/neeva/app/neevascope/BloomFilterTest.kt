package com.neeva.app.neevascope

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import org.junit.Test
import org.junit.runner.RunWith
import org.yaml.snakeyaml.Yaml
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class BloomFilterTest : BaseTest() {
    @Test
    fun stripMobile() {
        val cases = mapOf(
            "https://en.m.wikipedia.org" to "https://en.wikipedia.org",
            "https://mobile.facebook.com" to "https://www.facebook.com",
            "https://m.hotnews.ro" to "https://www.hotnews.ro",
            "https://m.youtube.com" to "https://www.youtube.com",
            "https://news.ycombinator.com" to "https://news.ycombinator.com"
        )
        cases.forEach { case ->
            expectThat(CanonicalUrl().stripMobile(Uri.parse(case.key)))
                .isEqualTo(Uri.parse(case.value))
        }
    }

    @Test
    fun canonicalizeUrl_with_canonical_url_v3() {
        val file = this.javaClass.classLoader?.getResourceAsStream("canonical_url_v3.yaml")
        val yaml = Yaml()
        val parsed: ArrayList<Map<String, String>> = yaml.load(file)

        parsed.forEach {
            val input = Uri.parse(it["url"])
            val expectedOutput = Uri.parse(it["out"])
            expectThat(CanonicalUrl().canonicalizeUrl(input)).isEqualTo(expectedOutput)
        }
    }
}
