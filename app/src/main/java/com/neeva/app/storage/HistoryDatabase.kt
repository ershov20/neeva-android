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
import com.neeva.app.storage.daos.SpaceDao
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.storage.entities.Visit

@Database(
    entities = [Site::class, Visit::class, SpaceItem::class, Space::class],
    version = 11,
    autoMigrations = [
        AutoMigration(from = 6, to = 7, spec = HistoryDatabase.MigrationFrom6To7::class),
        AutoMigration(from = 7, to = 8, spec = HistoryDatabase.MigrationFrom7To8::class),
        AutoMigration(from = 8, to = 9, spec = HistoryDatabase.MigrationFrom8To9::class),
        AutoMigration(from = 9, to = 10, spec = HistoryDatabase.MigrationFrom9To10::class),
        AutoMigration(from = 10, to = 11, spec = HistoryDatabase.MigrationFrom10To11::class),
    ]
)
@TypeConverters(com.neeva.app.storage.TypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): HistoryDao
    abstract fun spaceDao(): SpaceDao

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

    @DeleteColumn.Entries(
        DeleteColumn(tableName = "Visit", columnName = "visitRootID"),
        DeleteColumn(tableName = "Visit", columnName = "visitType")
    )
    class MigrationFrom8To9 : AutoMigrationSpec

    class MigrationFrom9To10 : AutoMigrationSpec

    class MigrationFrom10To11 : AutoMigrationSpec
}
