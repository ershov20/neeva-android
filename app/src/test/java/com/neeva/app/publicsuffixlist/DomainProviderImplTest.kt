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
class DomainProviderImplTest: BaseTest() {
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
        expectThat(domainProviderImpl.loadingState.value).isEqualTo(DomainProviderImpl.LoadingState.READY)

        expectThat(domainProviderImpl.getRegisteredDomainForHost("neeva.com")).isEqualTo("neeva.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("subdomain.neeva.com")).isEqualTo("neeva.com")

        expectThat(domainProviderImpl.getRegisteredDomain(Uri.parse("https://neeva.com"))).isEqualTo("neeva.com")
        expectThat(domainProviderImpl.getRegisteredDomain(Uri.parse("https://subdomain.neeva.com"))).isEqualTo("neeva.com")

        // Adapted from https://raw.githubusercontent.com/publicsuffix/list/master/tests/test_psl.txt
        // Commented out tests were already commented out when the test was adapted.

        // null input.
        expectThat(domainProviderImpl.getRegisteredDomainForHost(null)).isEqualTo(null)

        // Mixed case.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("COM")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("example.COM")).isEqualTo("example.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("WwW.example.COM")).isEqualTo("example.com")

        // Leading dot.
        expectThat(domainProviderImpl.getRegisteredDomainForHost(".com")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost(".example")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost(".example.com")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost(".example.example")).isEqualTo(null)

        // Unlisted TLD.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("example")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("example.example")).isEqualTo("example.example")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.example.example")).isEqualTo("example.example")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.example.example")).isEqualTo("example.example")

        // Listed, but non-Internet, TLD.
        //expectThat(suffixListManager.findLongestSuffix("local")).isEqualTo(null)
        //expectThat(suffixListManager.findLongestSuffix("example.local")).isEqualTo(null)
        //expectThat(suffixListManager.findLongestSuffix("b.example.local")).isEqualTo(null)
        //expectThat(suffixListManager.findLongestSuffix("a.b.example.local")).isEqualTo(null)

        // TLD with only 1 rule.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("biz")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("domain.biz")).isEqualTo("domain.biz")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.domain.biz")).isEqualTo("domain.biz")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.domain.biz")).isEqualTo("domain.biz")

        // TLD with some 2-level rules.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("com")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("example.com")).isEqualTo("example.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.example.com")).isEqualTo("example.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.example.com")).isEqualTo("example.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("uk.com")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("example.uk.com")).isEqualTo("example.uk.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.example.uk.com")).isEqualTo("example.uk.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.example.uk.com")).isEqualTo("example.uk.com")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.ac")).isEqualTo("test.ac")

        // TLD with only 1 (wildcard) rule.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("mm")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("c.mm")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.c.mm")).isEqualTo("b.c.mm")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.c.mm")).isEqualTo("b.c.mm")

        // More complex TLD.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("jp")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.jp")).isEqualTo("test.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.test.jp")).isEqualTo("test.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("ac.jp")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.ac.jp")).isEqualTo("test.ac.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.test.ac.jp")).isEqualTo("test.ac.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("kyoto.jp")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.kyoto.jp")).isEqualTo("test.kyoto.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("ide.kyoto.jp")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.ide.kyoto.jp")).isEqualTo("b.ide.kyoto.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.ide.kyoto.jp")).isEqualTo("b.ide.kyoto.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("c.kobe.jp")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.c.kobe.jp")).isEqualTo("b.c.kobe.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.c.kobe.jp")).isEqualTo("b.c.kobe.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("city.kobe.jp")).isEqualTo("city.kobe.jp")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.city.kobe.jp")).isEqualTo("city.kobe.jp")

        // TLD with a wildcard rule and exceptions.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("ck")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.ck")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("b.test.ck")).isEqualTo("b.test.ck")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("a.b.test.ck")).isEqualTo("b.test.ck")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.ck")).isEqualTo("www.ck")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.www.ck")).isEqualTo("www.ck")

        // US K12.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("us")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.us")).isEqualTo("test.us")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.test.us")).isEqualTo("test.us")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("ak.us")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.ak.us")).isEqualTo("test.ak.us")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.test.ak.us")).isEqualTo("test.ak.us")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("k12.ak.us")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("test.k12.ak.us")).isEqualTo("test.k12.ak.us")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.test.k12.ak.us")).isEqualTo("test.k12.ak.us")

        // IDN labels.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("食狮.com.cn")).isEqualTo("食狮.com.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("食狮.公司.cn")).isEqualTo("食狮.公司.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.食狮.公司.cn")).isEqualTo("食狮.公司.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("shishi.公司.cn")).isEqualTo("shishi.公司.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("公司.cn")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("食狮.中国")).isEqualTo("食狮.中国")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.食狮.中国")).isEqualTo("食狮.中国")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("shishi.中国")).isEqualTo("shishi.中国")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("中国")).isEqualTo(null)

        // Same as above, but punycoded.
        expectThat(domainProviderImpl.getRegisteredDomainForHost("xn--85x722f.com.cn")).isEqualTo("xn--85x722f.com.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("xn--85x722f.xn--55qx5d.cn")).isEqualTo("xn--85x722f.xn--55qx5d.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.xn--85x722f.xn--55qx5d.cn")).isEqualTo("xn--85x722f.xn--55qx5d.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("shishi.xn--55qx5d.cn")).isEqualTo("shishi.xn--55qx5d.cn")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("xn--55qx5d.cn")).isEqualTo(null)
        expectThat(domainProviderImpl.getRegisteredDomainForHost("xn--85x722f.xn--fiqs8s")).isEqualTo("xn--85x722f.xn--fiqs8s")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("www.xn--85x722f.xn--fiqs8s")).isEqualTo("xn--85x722f.xn--fiqs8s")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("shishi.xn--fiqs8s")).isEqualTo("shishi.xn--fiqs8s")
        expectThat(domainProviderImpl.getRegisteredDomainForHost("xn--fiqs8s")).isEqualTo(null)
    }
}
