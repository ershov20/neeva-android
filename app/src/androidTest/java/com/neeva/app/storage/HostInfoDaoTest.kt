package com.neeva.app.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.entities.HostInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@OptIn(ExperimentalCoroutinesApi::class)
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
            val hosts = database.hostInfoDao().getAllTrackingAllowedHosts()

            expectThat(hosts).hasSize(2)
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostA")?.host
            ).isEqualTo("hostA")
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostB")?.host
            ).isEqualTo("hostB")
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostC")?.host
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
            val hosts = database.hostInfoDao().getAllTrackingAllowedHosts()

            expectThat(hosts).hasSize(3)
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostA")?.host
            ).isEqualTo("hostA")
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostB")?.host
            ).isEqualTo("hostB")
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostC")?.host
            ).isEqualTo("hostC")

            database.hostInfoDao().deleteFromHostInfo("hostA")

            val hostsAfterDeletion = database.hostInfoDao().getAllTrackingAllowedHosts()

            expectThat(hostsAfterDeletion).hasSize(2)
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostA")?.host
            ).isNull()
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostB")?.host
            ).isEqualTo("hostB")
            expectThat(
                database.hostInfoDao().getHostInfoByName("hostC")?.host
            ).isEqualTo("hostC")

            database.hostInfoDao().deleteTrackingAllowedHosts()

            val hostsAfterAllDeletion = database.hostInfoDao().getAllTrackingAllowedHosts()

            expectThat(hostsAfterAllDeletion).hasSize(0)
        }
    }
}
