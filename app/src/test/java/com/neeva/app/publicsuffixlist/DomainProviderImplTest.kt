package com.neeva.app.publicsuffixlist

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
class DomainProviderImplTest : BaseTest() {
    private lateinit var domainProviderImpl: DomainProviderImpl

    override fun setUp() {
        super.setUp()
        domainProviderImpl = DomainProviderImpl(RuntimeEnvironment.getApplication())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun baseTest() = runTest {
        domainProviderImpl.initialize()
        advanceUntilIdle()
        expectThat(domainProviderImpl.loadingState.value)
            .isEqualTo(DomainProviderImpl.LoadingState.READY)

        fun domainForHost(host: String?) = domainProviderImpl.getRegisteredDomainForHost(host)

        expectThat(domainForHost("neeva.com")).isEqualTo("neeva.com")
        expectThat(domainForHost("subdomain.neeva.com")).isEqualTo("neeva.com")

        expectThat(domainProviderImpl.getRegisteredDomain(Uri.parse("https://neeva.com")))
            .isEqualTo("neeva.com")
        expectThat(
            domainProviderImpl.getRegisteredDomain(Uri.parse("https://subdomain.neeva.com"))
        ).isEqualTo("neeva.com")

        // Adapted from https://raw.githubusercontent.com/publicsuffix/list/master/tests/test_psl.txt
        // Commented out tests were already commented out when the test was adapted.

        // null input.
        expectThat(domainForHost(null)).isEqualTo(null)

        // Mixed case.
        expectThat(domainForHost("COM")).isEqualTo(null)
        expectThat(domainForHost("example.COM")).isEqualTo("example.com")
        expectThat(domainForHost("WwW.example.COM")).isEqualTo("example.com")

        // Leading dot.
        expectThat(domainForHost(".com")).isEqualTo(null)
        expectThat(domainForHost(".example")).isEqualTo(null)
        expectThat(domainForHost(".example.com")).isEqualTo(null)
        expectThat(domainForHost(".example.example")).isEqualTo(null)

        // Unlisted TLD.
        expectThat(domainForHost("example")).isEqualTo(null)
        expectThat(domainForHost("example.example")).isEqualTo("example.example")
        expectThat(domainForHost("b.example.example")).isEqualTo("example.example")
        expectThat(domainForHost("a.b.example.example")).isEqualTo("example.example")

        // Listed, but non-Internet, TLD.
        // expectThat(suffixListManager.findLongestSuffix("local")).isEqualTo(null)
        // expectThat(suffixListManager.findLongestSuffix("example.local")).isEqualTo(null)
        // expectThat(suffixListManager.findLongestSuffix("b.example.local")).isEqualTo(null)
        // expectThat(suffixListManager.findLongestSuffix("a.b.example.local")).isEqualTo(null)

        // TLD with only 1 rule.
        expectThat(domainForHost("biz")).isEqualTo(null)
        expectThat(domainForHost("domain.biz")).isEqualTo("domain.biz")
        expectThat(domainForHost("b.domain.biz")).isEqualTo("domain.biz")
        expectThat(domainForHost("a.b.domain.biz")).isEqualTo("domain.biz")

        // TLD with some 2-level rules.
        expectThat(domainForHost("com")).isEqualTo(null)
        expectThat(domainForHost("example.com")).isEqualTo("example.com")
        expectThat(domainForHost("b.example.com")).isEqualTo("example.com")
        expectThat(domainForHost("a.b.example.com")).isEqualTo("example.com")
        expectThat(domainForHost("uk.com")).isEqualTo(null)
        expectThat(domainForHost("example.uk.com")).isEqualTo("example.uk.com")
        expectThat(domainForHost("b.example.uk.com")).isEqualTo("example.uk.com")
        expectThat(domainForHost("a.b.example.uk.com")).isEqualTo("example.uk.com")
        expectThat(domainForHost("test.ac")).isEqualTo("test.ac")

        // TLD with only 1 (wildcard) rule.
        expectThat(domainForHost("mm")).isEqualTo(null)
        expectThat(domainForHost("c.mm")).isEqualTo(null)
        expectThat(domainForHost("b.c.mm")).isEqualTo("b.c.mm")
        expectThat(domainForHost("a.b.c.mm")).isEqualTo("b.c.mm")

        // More complex TLD.
        expectThat(domainForHost("jp")).isEqualTo(null)
        expectThat(domainForHost("test.jp")).isEqualTo("test.jp")
        expectThat(domainForHost("www.test.jp")).isEqualTo("test.jp")
        expectThat(domainForHost("ac.jp")).isEqualTo(null)
        expectThat(domainForHost("test.ac.jp")).isEqualTo("test.ac.jp")
        expectThat(domainForHost("www.test.ac.jp")).isEqualTo("test.ac.jp")
        expectThat(domainForHost("kyoto.jp")).isEqualTo(null)
        expectThat(domainForHost("test.kyoto.jp")).isEqualTo("test.kyoto.jp")
        expectThat(domainForHost("ide.kyoto.jp")).isEqualTo(null)
        expectThat(domainForHost("b.ide.kyoto.jp")).isEqualTo("b.ide.kyoto.jp")
        expectThat(domainForHost("a.b.ide.kyoto.jp")).isEqualTo("b.ide.kyoto.jp")
        expectThat(domainForHost("c.kobe.jp")).isEqualTo(null)
        expectThat(domainForHost("b.c.kobe.jp")).isEqualTo("b.c.kobe.jp")
        expectThat(domainForHost("a.b.c.kobe.jp")).isEqualTo("b.c.kobe.jp")
        expectThat(domainForHost("city.kobe.jp")).isEqualTo("city.kobe.jp")
        expectThat(domainForHost("www.city.kobe.jp")).isEqualTo("city.kobe.jp")

        // TLD with a wildcard rule and exceptions.
        expectThat(domainForHost("ck")).isEqualTo(null)
        expectThat(domainForHost("test.ck")).isEqualTo(null)
        expectThat(domainForHost("b.test.ck")).isEqualTo("b.test.ck")
        expectThat(domainForHost("a.b.test.ck")).isEqualTo("b.test.ck")
        expectThat(domainForHost("www.ck")).isEqualTo("www.ck")
        expectThat(domainForHost("www.www.ck")).isEqualTo("www.ck")

        // US K12.
        expectThat(domainForHost("us")).isEqualTo(null)
        expectThat(domainForHost("test.us")).isEqualTo("test.us")
        expectThat(domainForHost("www.test.us")).isEqualTo("test.us")
        expectThat(domainForHost("ak.us")).isEqualTo(null)
        expectThat(domainForHost("test.ak.us")).isEqualTo("test.ak.us")
        expectThat(domainForHost("www.test.ak.us")).isEqualTo("test.ak.us")
        expectThat(domainForHost("k12.ak.us")).isEqualTo(null)
        expectThat(domainForHost("test.k12.ak.us")).isEqualTo("test.k12.ak.us")
        expectThat(domainForHost("www.test.k12.ak.us")).isEqualTo("test.k12.ak.us")

        // IDN labels.
        expectThat(domainForHost("食狮.com.cn")).isEqualTo("食狮.com.cn")
        expectThat(domainForHost("食狮.公司.cn")).isEqualTo("食狮.公司.cn")
        expectThat(domainForHost("www.食狮.公司.cn")).isEqualTo("食狮.公司.cn")
        expectThat(domainForHost("shishi.公司.cn")).isEqualTo("shishi.公司.cn")
        expectThat(domainForHost("公司.cn")).isEqualTo(null)
        expectThat(domainForHost("食狮.中国")).isEqualTo("食狮.中国")
        expectThat(domainForHost("www.食狮.中国")).isEqualTo("食狮.中国")
        expectThat(domainForHost("shishi.中国")).isEqualTo("shishi.中国")
        expectThat(domainForHost("中国")).isEqualTo(null)

        // Same as above, but punycoded.
        expectThat(domainForHost("xn--85x722f.com.cn")).isEqualTo("xn--85x722f.com.cn")
        expectThat(domainForHost("xn--85x722f.xn--55qx5d.cn"))
            .isEqualTo("xn--85x722f.xn--55qx5d.cn")
        expectThat(domainForHost("www.xn--85x722f.xn--55qx5d.cn"))
            .isEqualTo("xn--85x722f.xn--55qx5d.cn")
        expectThat(domainForHost("shishi.xn--55qx5d.cn")).isEqualTo("shishi.xn--55qx5d.cn")
        expectThat(domainForHost("xn--55qx5d.cn")).isEqualTo(null)
        expectThat(domainForHost("xn--85x722f.xn--fiqs8s")).isEqualTo("xn--85x722f.xn--fiqs8s")
        expectThat(domainForHost("www.xn--85x722f.xn--fiqs8s"))
            .isEqualTo("xn--85x722f.xn--fiqs8s")
        expectThat(domainForHost("shishi.xn--fiqs8s")).isEqualTo("shishi.xn--fiqs8s")
        expectThat(domainForHost("xn--fiqs8s")).isEqualTo(null)
    }
}
