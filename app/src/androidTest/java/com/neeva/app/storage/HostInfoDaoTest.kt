package com.neeva.app.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.entities.HostInfo
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class HostInfoDaoTest : HistoryDatabaseBaseTest() {
    private lateinit var hostInfoRepository: HostInfoDao

    override fun setUp() {
        super.setUp()
        hostInfoRepository = database.hostInfoDao()
    }

    @Test
    fun insert() {
        runBlocking {
            hostInfoRepository.upsert(
                HostInfo(host = "hostA", isTrackingAllowed = true)
            )
            hostInfoRepository.upsert(
                HostInfo(host = "hostA", isTrackingAllowed = true)
            )
            hostInfoRepository.upsert(
                HostInfo(host = "hostB", isTrackingAllowed = true)
            )
            val hosts = hostInfoRepository.getAllTrackingAllowedHosts()

            expectThat(hosts).hasSize(2)
            expectThat(
                hostInfoRepository.getHostInfoByName("hostA")?.host
            ).isEqualTo("hostA")
            expectThat(
                hostInfoRepository.getHostInfoByName("hostB")?.host
            ).isEqualTo("hostB")
            expectThat(
                hostInfoRepository.getHostInfoByName("hostC")?.host
            ).isNull()
        }
    }

    @Test
    fun delete() {
        runBlocking {
            hostInfoRepository.upsert(
                HostInfo(host = "hostA", isTrackingAllowed = true)
            )
            hostInfoRepository.upsert(
                HostInfo(host = "hostB", isTrackingAllowed = true)
            )
            hostInfoRepository.upsert(
                HostInfo(host = "hostC", isTrackingAllowed = true)
            )
            val hosts = hostInfoRepository.getAllTrackingAllowedHosts()

            expectThat(hosts).hasSize(3)
            expectThat(
                hostInfoRepository.getHostInfoByName("hostA")?.host
            ).isEqualTo("hostA")
            expectThat(
                hostInfoRepository.getHostInfoByName("hostB")?.host
            ).isEqualTo("hostB")
            expectThat(
                hostInfoRepository.getHostInfoByName("hostC")?.host
            ).isEqualTo("hostC")

            hostInfoRepository.deleteFromHostInfo("hostA")

            val hostsAfterDeletion = hostInfoRepository.getAllTrackingAllowedHosts()

            expectThat(hostsAfterDeletion).hasSize(2)
            expectThat(
                hostInfoRepository.getHostInfoByName("hostA")?.host
            ).isNull()
            expectThat(
                hostInfoRepository.getHostInfoByName("hostB")?.host
            ).isEqualTo("hostB")
            expectThat(
                hostInfoRepository.getHostInfoByName("hostC")?.host
            ).isEqualTo("hostC")

            hostInfoRepository.deleteTrackingAllowedHosts()

            val hostsAfterAllDeletion = hostInfoRepository.getAllTrackingAllowedHosts()

            expectThat(hostsAfterAllDeletion).hasSize(0)
        }
    }

    @Test
    fun toggleTrackingAllowedForHost() {
        runBlocking {
            val host = "example.com"

            expectThat(hostInfoRepository.getHostInfoByName(host)).isNull()

            hostInfoRepository.toggleTrackingAllowedForHost(host)
            expectThat(hostInfoRepository.getHostInfoByName(host)?.isTrackingAllowed).isTrue()

            hostInfoRepository.toggleTrackingAllowedForHost(host)
            expectThat(hostInfoRepository.getHostInfoByName(host)).isNull()
        }
    }
}
