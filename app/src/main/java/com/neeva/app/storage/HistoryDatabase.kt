package com.neeva.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Domain::class, Site::class, Visit::class], version = 6)
@TypeConverters(com.neeva.app.storage.TypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun fromDomains(): DomainAccessor
    abstract fun fromSites(): SitesWithVisitsAccessor

    companion object {
        fun create(context: Context): HistoryDatabase {
            return Room
                .databaseBuilder(context, HistoryDatabase::class.java, "HistoryDB")
                .createFromAsset("database/cached_domains.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
