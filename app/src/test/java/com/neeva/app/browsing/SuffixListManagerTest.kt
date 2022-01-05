package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
@Config(
    // TODO(dan.alcantara): Figure out why this has to point at src/main even when I change the asset parameter
    manifest = "src/main/EmptyManifest.xml"
)
class SuffixListManagerTest: BaseTest() {
    private lateinit var suffixListManager: SuffixListManager

    override fun setUp() {
        super.setUp()
        suffixListManager = SuffixListManager()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun baseTest() = runTest {
        suffixListManager.initialize(RuntimeEnvironment.getApplication())
        advanceUntilIdle()
        expectThat(suffixListManager.loadingState.value).isEqualTo(SuffixListManager.LoadingState.READY)

        expectThat(suffixListManager.getRegisteredDomain("neeva.com")).isEqualTo("neeva.com")
        expectThat(suffixListManager.getRegisteredDomain("subdomain.neeva.com")).isEqualTo("neeva.com")

        expectThat(suffixListManager.getRegisteredDomain(Uri.parse("https://neeva.com"))).isEqualTo("neeva.com")
        expectThat(suffixListManager.getRegisteredDomain(Uri.parse("https://subdomain.neeva.com"))).isEqualTo("neeva.com")

        // Adapted from https://raw.githubusercontent.com/publicsuffix/list/master/tests/test_psl.txt
        // Commented out tests were already commented out when the test was adapted.

        // null input.
        expectThat(suffixListManager.getRegisteredDomain(null)).isEqualTo(null)

        // Mixed case.
        expectThat(suffixListManager.getRegisteredDomain("COM")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("example.COM")).isEqualTo("example.com")
        expectThat(suffixListManager.getRegisteredDomain("WwW.example.COM")).isEqualTo("example.com")

        // Leading dot.
        expectThat(suffixListManager.getRegisteredDomain(".com")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain(".example")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain(".example.com")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain(".example.example")).isEqualTo(null)

        // Unlisted TLD.
        expectThat(suffixListManager.getRegisteredDomain("example")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("example.example")).isEqualTo("example.example")
        expectThat(suffixListManager.getRegisteredDomain("b.example.example")).isEqualTo("example.example")
        expectThat(suffixListManager.getRegisteredDomain("a.b.example.example")).isEqualTo("example.example")

        // Listed, but non-Internet, TLD.
        //expectThat(suffixListManager.findLongestSuffix("local")).isEqualTo(null)
        //expectThat(suffixListManager.findLongestSuffix("example.local")).isEqualTo(null)
        //expectThat(suffixListManager.findLongestSuffix("b.example.local")).isEqualTo(null)
        //expectThat(suffixListManager.findLongestSuffix("a.b.example.local")).isEqualTo(null)

        // TLD with only 1 rule.
        expectThat(suffixListManager.getRegisteredDomain("biz")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("domain.biz")).isEqualTo("domain.biz")
        expectThat(suffixListManager.getRegisteredDomain("b.domain.biz")).isEqualTo("domain.biz")
        expectThat(suffixListManager.getRegisteredDomain("a.b.domain.biz")).isEqualTo("domain.biz")

        // TLD with some 2-level rules.
        expectThat(suffixListManager.getRegisteredDomain("com")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("example.com")).isEqualTo("example.com")
        expectThat(suffixListManager.getRegisteredDomain("b.example.com")).isEqualTo("example.com")
        expectThat(suffixListManager.getRegisteredDomain("a.b.example.com")).isEqualTo("example.com")
        expectThat(suffixListManager.getRegisteredDomain("uk.com")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("example.uk.com")).isEqualTo("example.uk.com")
        expectThat(suffixListManager.getRegisteredDomain("b.example.uk.com")).isEqualTo("example.uk.com")
        expectThat(suffixListManager.getRegisteredDomain("a.b.example.uk.com")).isEqualTo("example.uk.com")
        expectThat(suffixListManager.getRegisteredDomain("test.ac")).isEqualTo("test.ac")

        // TLD with only 1 (wildcard) rule.
        expectThat(suffixListManager.getRegisteredDomain("mm")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("c.mm")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("b.c.mm")).isEqualTo("b.c.mm")
        expectThat(suffixListManager.getRegisteredDomain("a.b.c.mm")).isEqualTo("b.c.mm")

        // More complex TLD.
        expectThat(suffixListManager.getRegisteredDomain("jp")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("test.jp")).isEqualTo("test.jp")
        expectThat(suffixListManager.getRegisteredDomain("www.test.jp")).isEqualTo("test.jp")
        expectThat(suffixListManager.getRegisteredDomain("ac.jp")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("test.ac.jp")).isEqualTo("test.ac.jp")
        expectThat(suffixListManager.getRegisteredDomain("www.test.ac.jp")).isEqualTo("test.ac.jp")
        expectThat(suffixListManager.getRegisteredDomain("kyoto.jp")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("test.kyoto.jp")).isEqualTo("test.kyoto.jp")
        expectThat(suffixListManager.getRegisteredDomain("ide.kyoto.jp")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("b.ide.kyoto.jp")).isEqualTo("b.ide.kyoto.jp")
        expectThat(suffixListManager.getRegisteredDomain("a.b.ide.kyoto.jp")).isEqualTo("b.ide.kyoto.jp")
        expectThat(suffixListManager.getRegisteredDomain("c.kobe.jp")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("b.c.kobe.jp")).isEqualTo("b.c.kobe.jp")
        expectThat(suffixListManager.getRegisteredDomain("a.b.c.kobe.jp")).isEqualTo("b.c.kobe.jp")
        expectThat(suffixListManager.getRegisteredDomain("city.kobe.jp")).isEqualTo("city.kobe.jp")
        expectThat(suffixListManager.getRegisteredDomain("www.city.kobe.jp")).isEqualTo("city.kobe.jp")

        // TLD with a wildcard rule and exceptions.
        expectThat(suffixListManager.getRegisteredDomain("ck")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("test.ck")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("b.test.ck")).isEqualTo("b.test.ck")
        expectThat(suffixListManager.getRegisteredDomain("a.b.test.ck")).isEqualTo("b.test.ck")
        expectThat(suffixListManager.getRegisteredDomain("www.ck")).isEqualTo("www.ck")
        expectThat(suffixListManager.getRegisteredDomain("www.www.ck")).isEqualTo("www.ck")

        // US K12.
        expectThat(suffixListManager.getRegisteredDomain("us")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("test.us")).isEqualTo("test.us")
        expectThat(suffixListManager.getRegisteredDomain("www.test.us")).isEqualTo("test.us")
        expectThat(suffixListManager.getRegisteredDomain("ak.us")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("test.ak.us")).isEqualTo("test.ak.us")
        expectThat(suffixListManager.getRegisteredDomain("www.test.ak.us")).isEqualTo("test.ak.us")
        expectThat(suffixListManager.getRegisteredDomain("k12.ak.us")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("test.k12.ak.us")).isEqualTo("test.k12.ak.us")
        expectThat(suffixListManager.getRegisteredDomain("www.test.k12.ak.us")).isEqualTo("test.k12.ak.us")

        // IDN labels.
        expectThat(suffixListManager.getRegisteredDomain("食狮.com.cn")).isEqualTo("食狮.com.cn")
        expectThat(suffixListManager.getRegisteredDomain("食狮.公司.cn")).isEqualTo("食狮.公司.cn")
        expectThat(suffixListManager.getRegisteredDomain("www.食狮.公司.cn")).isEqualTo("食狮.公司.cn")
        expectThat(suffixListManager.getRegisteredDomain("shishi.公司.cn")).isEqualTo("shishi.公司.cn")
        expectThat(suffixListManager.getRegisteredDomain("公司.cn")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("食狮.中国")).isEqualTo("食狮.中国")
        expectThat(suffixListManager.getRegisteredDomain("www.食狮.中国")).isEqualTo("食狮.中国")
        expectThat(suffixListManager.getRegisteredDomain("shishi.中国")).isEqualTo("shishi.中国")
        expectThat(suffixListManager.getRegisteredDomain("中国")).isEqualTo(null)

        // Same as above, but punycoded.
        expectThat(suffixListManager.getRegisteredDomain("xn--85x722f.com.cn")).isEqualTo("xn--85x722f.com.cn")
        expectThat(suffixListManager.getRegisteredDomain("xn--85x722f.xn--55qx5d.cn")).isEqualTo("xn--85x722f.xn--55qx5d.cn")
        expectThat(suffixListManager.getRegisteredDomain("www.xn--85x722f.xn--55qx5d.cn")).isEqualTo("xn--85x722f.xn--55qx5d.cn")
        expectThat(suffixListManager.getRegisteredDomain("shishi.xn--55qx5d.cn")).isEqualTo("shishi.xn--55qx5d.cn")
        expectThat(suffixListManager.getRegisteredDomain("xn--55qx5d.cn")).isEqualTo(null)
        expectThat(suffixListManager.getRegisteredDomain("xn--85x722f.xn--fiqs8s")).isEqualTo("xn--85x722f.xn--fiqs8s")
        expectThat(suffixListManager.getRegisteredDomain("www.xn--85x722f.xn--fiqs8s")).isEqualTo("xn--85x722f.xn--fiqs8s")
        expectThat(suffixListManager.getRegisteredDomain("shishi.xn--fiqs8s")).isEqualTo("shishi.xn--fiqs8s")
        expectThat(suffixListManager.getRegisteredDomain("xn--fiqs8s")).isEqualTo(null)
    }
}
