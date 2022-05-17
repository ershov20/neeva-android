package com.neeva.app.storage

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.neeva.app.Dispatchers
import com.neeva.app.ZipUtils
import com.neeva.app.storage.daos.HistoryDao
import com.neeva.app.storage.daos.SpaceDao
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.storage.entities.Visit
import java.io.File
import java.util.Date
import kotlinx.coroutines.withContext

@Database(
    entities = [Site::class, Visit::class, SpaceItem::class, Space::class],
    version = 12,
    autoMigrations = [
        AutoMigration(from = 6, to = 7, spec = HistoryDatabase.MigrationFrom6To7::class),
        AutoMigration(from = 7, to = 8, spec = HistoryDatabase.MigrationFrom7To8::class),
        AutoMigration(from = 8, to = 9, spec = HistoryDatabase.MigrationFrom8To9::class),
        AutoMigration(from = 9, to = 10, spec = HistoryDatabase.MigrationFrom9To10::class),
        AutoMigration(from = 10, to = 11, spec = HistoryDatabase.MigrationFrom10To11::class),
        AutoMigration(from = 11, to = 12, spec = HistoryDatabase.MigrationFrom11To12::class),
    ]
)
@TypeConverters(com.neeva.app.storage.TypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): HistoryDao
    abstract fun spaceDao(): SpaceDao

    companion object {
        private val TAG = HistoryDatabase::class.simpleName

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

    class MigrationFrom11To12 : AutoMigrationSpec

    suspend fun export(
        context: Context,
        dispatchers: Dispatchers
    ) = withContext(dispatchers.io) {
        val databaseFile = File(openHelper.readableDatabase.path).parentFile
            ?: run {
                Log.e(TAG, "Failed to find database")
                return@withContext
            }

        try {
            // Android doesn't let us share files directly out of the actual database directory, so
            // we zip it up and put it into a readable cache directory, instead.
            // See shared_file_paths.xml.
            val exported = File(context.cacheDir, "exported")
            exported.mkdirs()

            val exportedFile = File(exported, "database.zip")
            if (!ZipUtils.compress(databaseFile, exportedFile)) {
                Log.e(TAG, "Failed to zip file")
                return@withContext
            }

            // This authority has to match what is defined in the AndroidManifest.xml.
            val authority = context.packageName + ".fileprovider"

            // Get the shareable content URI for the file and fire out an Intent to any app the user
            // might want to send it to.
            val uri = FileProvider.getUriForFile(context, authority, exportedFile)
            val sendIntent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, "Database export ${Date()}")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType("*/*")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export database", e)
            return@withContext
        }
    }
}
