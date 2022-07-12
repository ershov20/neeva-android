package com.neeva.app.cookiecutter

import com.neeva.app.BaseTest
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

/**
 * Tests that the Tracking data creation working properly
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackingDataTest : BaseTest() {
    private lateinit var domainProviderImpl: DomainProviderImpl

    override fun setUp() {
        super.setUp()
        domainProviderImpl = DomainProviderImpl(RuntimeEnvironment.getApplication())
    }

    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun testSingleDomainTrackingDataCreation() {
        val singleDomainTrackingData = TrackingData.create(
            mapOf("www.testing.com" to 5), domainProviderImpl
        )

        expectThat(singleDomainTrackingData.numDomains).isEqualTo(1)
        expectThat(singleDomainTrackingData.numTrackers).isEqualTo(5)
        expectThat(singleDomainTrackingData.trackingEntities.size).isEqualTo(0)
    }

    @Test
    fun testMultiDomainTrackingDataCreation() {
        val multiDomainTrackingData = TrackingData.create(
            mapOf(
                "a.testing.com" to 1,
                "b.testing.com" to 2,
                "c.testing.com" to 3,
            ),
            domainProviderImpl
        )

        expectThat(multiDomainTrackingData.numDomains).isEqualTo(3)
        expectThat(multiDomainTrackingData.numTrackers).isEqualTo(6)
        expectThat(multiDomainTrackingData.trackingEntities.size).isEqualTo(0)
    }

    @Test
    fun testMultiDomainTrackingDataCreationWithEntities() {
        val multiDomainWithWhosTrackingYouEntityData = TrackingData.create(
            mapOf(
                "a.googleadservices.com" to 1,
                "b.googleadservices.com" to 2,
                "c.googleadsserving.cn" to 3,
                "a.yahoo.com" to 4,
                "b.yahoo.com" to 5,
                "a.testing.com" to 6,
            ),
            domainProviderImpl
        )

        expectThat(
            multiDomainWithWhosTrackingYouEntityData.numDomains
        ).isEqualTo(6)
        expectThat(
            multiDomainWithWhosTrackingYouEntityData.numTrackers
        ).isEqualTo(21)
        expectThat(
            multiDomainWithWhosTrackingYouEntityData.trackingEntities.size
        ).isEqualTo(2)
        expectThat(
            multiDomainWithWhosTrackingYouEntityData.trackingEntities.keys.contains(
                TrackingEntity.VERIZONMEDIA
            )
        ).isTrue()
        expectThat(
            multiDomainWithWhosTrackingYouEntityData.trackingEntities.keys.contains(
                TrackingEntity.GOOGLE
            )
        ).isTrue()
        expectThat(
            multiDomainWithWhosTrackingYouEntityData.trackingEntities[TrackingEntity.VERIZONMEDIA]
        ).isEqualTo(9)
        expectThat(
            multiDomainWithWhosTrackingYouEntityData.trackingEntities[TrackingEntity.GOOGLE]
        ).isEqualTo(6)
    }

    @Test
    fun testWhoIsTrackingYouHosts() {
        val testWhoIsTrackingYouTrackingData = TrackingData.create(
            mapOf(
                "1emn.com" to 100,
                "accountkit.com" to 99,
                "ads-twitter.com" to 98,
                "alexa.com" to 3,
                "ligatus.com" to 2
            ),
            domainProviderImpl
        )

        expectThat(testWhoIsTrackingYouTrackingData.numTrackers).isEqualTo(302)
        expectThat(testWhoIsTrackingYouTrackingData.numDomains).isEqualTo(5)
        expectThat(testWhoIsTrackingYouTrackingData.trackingEntities.size).isEqualTo(5)

        val whoIsTrackingYouHosts = testWhoIsTrackingYouTrackingData.whoIsTrackingYouHosts()
        expectThat(whoIsTrackingYouHosts.size).isEqualTo(3)
        expectThat(whoIsTrackingYouHosts.contains(TrackingEntity.GOOGLE)).isTrue()
        expectThat(whoIsTrackingYouHosts.contains(TrackingEntity.FACEBOOK)).isTrue()
        expectThat(whoIsTrackingYouHosts.contains(TrackingEntity.TWITTER)).isTrue()
    }
}
