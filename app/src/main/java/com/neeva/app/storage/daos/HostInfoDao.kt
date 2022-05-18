package com.neeva.app.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.neeva.app.storage.entities.HostInfo

@Dao
interface HostInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(hostInfo: HostInfo)

    @Query("DELETE FROM HostInfo WHERE isTrackingAllowed = 1")
    suspend fun deleteTrackingAllowedHosts()

    @Query("DELETE FROM HostInfo WHERE host = :host")
    suspend fun deleteFromHostInfo(host: String)

    @Query("SELECT * FROM HostInfo WHERE isTrackingAllowed = 1")
    suspend fun getAllTrackingAllowedHosts(): List<HostInfo>

    @Query("SELECT * FROM HostInfo WHERE host = :host")
    suspend fun getHostInfoByName(host: String): HostInfo?

    @Transaction
    suspend fun upsert(hostInfo: HostInfo) {
        when (getHostInfoByName(hostInfo.host)) {
            null -> add(hostInfo)
            else -> null
        }
    }
}
