package com.neeva.app.storage

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neeva.app.NeevaBrowser

class History {
    companion object {
        val db = Room
            .databaseBuilder(
                NeevaBrowser.context,
                HistoryDatabase::class.java,
                "HistoryDB"
            )
            .createFromAsset("database/cached_domains.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}

@Database(entities = [Domain::class, Site::class, Visit::class], version = 6)
@TypeConverters(DateConverter::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun fromDomains(): DomainAccessor
    abstract fun fromSites(): SitesWithVisitsAccessor
}