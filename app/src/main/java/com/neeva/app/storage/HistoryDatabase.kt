package com.neeva.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neeva.app.storage.daos.DomainDao
import com.neeva.app.storage.daos.SitesWithVisitsAccessor
import com.neeva.app.storage.entities.Domain
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit

@Database(entities = [Domain::class, Site::class, Visit::class], version = 6)
@TypeConverters(com.neeva.app.storage.TypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun fromDomains(): DomainDao
    abstract fun fromSites(): SitesWithVisitsAccessor

    companion object {
        fun create(context: Context): HistoryDatabase {
            return Room
                .databaseBuilder(context, HistoryDatabase::class.java, "HistoryDB")
                .fallbackToDestructiveMigration()
                .build()
        }

        internal fun createInMemory(context: Context): HistoryDatabase {
            return Room
                .inMemoryDatabaseBuilder(context, HistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
