package com.neeva.app.neevascope

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import java.io.File
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

    @Test
    fun testFilter_with_passed_cases() {
        val testUrls = listOf(
            "https://imgur.com/UziCaVU",
            "https://www.macular.org/ultra-violet-and-blue-light",
            "https://osu.ppy.sh/b/1762724",
            "https://cero.bike/cero-one/",
            "https://m4dm4x.com/blf-q8-arrived/",
            "https://docs.microsoft.com/en-us/azure/devops/pipelines/build/triggers" +
                "?view=azure-devops&tabs=yaml",
            "https://www.nintendo.com/games/detail/pga-tour-2k21-switch",
            "http://www.dartmouth.edu/~chance/course/topics/curveball.html",
            "http://www.virtual-addiction.com",
            "https://aws.amazon.com/blogs/aws/new-ec2-auto-scaling-groups-with-multiple-" +
                "instance-types-purchase-options/",
            "https://chrome.google.com/webstore/detail/mosh/ooiklbnjmhbcgemelgfhaeaocllobloj",
            "https://islamophobianetwork.com",
            "https://www.youtube.com/watch?v=W171n_v4ZAs",
            "http://moinmo.in",
            "https://launcher.mojang.com/v1/objects/97b1c53df11cb8b973f4b522c8f4963b7e31495e/" +
                "server.jar",
            "https://www.youtube.com/watch?v=de1M4Q_g2eg",
            "http://en.wikipedia.org/wiki/Genius",
            "https://www.charlottesweb.com/all-charlottes-web-hemp-cbd-supplements",
            "https://www.sbnation.com/nba/2014/6/3/5772796/nba-y2k-series-finale-the-death-of-" +
                "basketball",
            "https://www.twitch.tv/streamsniper_hs_#stream",
            "https://youtu.be/qzuM2XTnpSA",
            "https://youtu.be/lpwG8f9nt4s",
            "http://bloodborne.wiki.fextralife.com/Anti-Clockwise+Metamorphosis",
            "http://teamfourstar.com/dragon-ball-z-abridged-episode-58-cell-mates/",
            "http://opencritic.com/critic/1496/philip-kollar",
            "http://www.id3.org",
            "https://store.finalfantasyxiv.com/ffxivstore/en-us/product/534",
            "https://store.steampowered.com/app/1073320/Meteorfall_Krumits_Tale/",
            "https://youtu.be/Lu6kQtxQbqU",
            "https://opencritic.com/critic/515/russell-archey",
            "https://pastebin.com/64GuVi2F/04457"
        )

        val file = File("src/test/resources/test_urls_out.bin")
        val filter = BloomFilter()
        filter.loadFilter(Uri.fromFile(file))

        testUrls.forEach { url ->
            expectThat(filter.mayContain(url)).isEqualTo(true)
        }
    }

    @Test
    fun testFilter_with_failed_cases() {
        val testUrls = listOf(
            "https://github.com/adiaddons/adibags/issues/332",
            "https://www.percona.com/blog/2017/10/23/mysql-point-in-time-recovery-right-way",
            "https://steamcommunity.com/sharedfiles/filedetails?id=152413570",
            "https://www.youtube.com/watch?v=wKd94xdVGWM",
            "https://www.lackadaisy.com/comic.php?comicid=1",
            "https://www.rand.org/pubs/monographs/MG840.html",
        )

        val file = File("src/test/resources/test_urls_out.bin")
        val filter = BloomFilter()
        filter.loadFilter(Uri.fromFile(file))

        testUrls.forEach { url ->
            expectThat(filter.mayContain(url)).isEqualTo(false)
        }
    }
}
