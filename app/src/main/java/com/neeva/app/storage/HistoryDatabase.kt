package com.neeva.app.storage

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.neeva.app.storage.daos.HistoryDao
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit

@Database(
    entities = [Site::class, Visit::class],
    version = 8,
    autoMigrations = [
        AutoMigration(from = 6, to = 7, spec = HistoryDatabase.MigrationFrom6To7::class),
        AutoMigration(from = 7, to = 8, spec = HistoryDatabase.MigrationFrom7To8::class)
    ]
)
@TypeConverters(com.neeva.app.storage.TypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): HistoryDao

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

    @DeleteTable.Entries(
        DeleteTable(tableName = "Domain")
    )
    class MigrationFrom7To8 : AutoMigrationSpec
}
