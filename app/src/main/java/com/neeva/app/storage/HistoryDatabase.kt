package com.neeva.app.storage

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.neeva.app.storage.daos.DomainDao
import com.neeva.app.storage.daos.SitesWithVisitsAccessor
import com.neeva.app.storage.entities.Domain
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit

@Database(
    entities = [Domain::class, Site::class, Visit::class],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 6, to = 7, spec = HistoryDatabase.MigrationFrom6To7::class)
    ]
)
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

    @DeleteColumn.Entries(
        DeleteColumn(tableName = "Domain", columnName = "encodedImage"),
        DeleteColumn(tableName = "Site", columnName = "visitCount"),
        DeleteColumn(tableName = "Site", columnName = "lastVisitTimestamp"),
        DeleteColumn(tableName = "Site", columnName = "encodedImage"),
        DeleteColumn(tableName = "Site", columnName = "description"),
        DeleteColumn(tableName = "Site", columnName = "entityType"),
        DeleteColumn(tableName = "Site", columnName = "imageURL"),
    )
    class MigrationFrom6To7 : AutoMigrationSpec
}
