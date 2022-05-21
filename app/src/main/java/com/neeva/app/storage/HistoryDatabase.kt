package com.neeva.app.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.util.Log
import androidx.annotation.WorkerThread
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
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.daos.HistoryDao
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.daos.SpaceDao
import com.neeva.app.storage.entities.HostInfo
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.storage.entities.Visit
import java.io.File
import java.util.Date
import kotlinx.coroutines.withContext

@Database(
    entities = [Site::class, Visit::class, SpaceItem::class, Space::class, HostInfo::class],
    version = 15,
    autoMigrations = [
        AutoMigration(from = 6, to = 7, spec = HistoryDatabase.MigrationFrom6To7::class),
        AutoMigration(from = 7, to = 8, spec = HistoryDatabase.MigrationFrom7To8::class),
        AutoMigration(from = 8, to = 9, spec = HistoryDatabase.MigrationFrom8To9::class),
        AutoMigration(from = 9, to = 10, spec = HistoryDatabase.MigrationFrom9To10::class),
        AutoMigration(from = 10, to = 11, spec = HistoryDatabase.MigrationFrom10To11::class),
        AutoMigration(from = 11, to = 12, spec = HistoryDatabase.MigrationFrom11To12::class),
        AutoMigration(from = 12, to = 13, spec = HistoryDatabase.MigrationFrom12To13::class),
        AutoMigration(from = 13, to = 14, spec = HistoryDatabase.MigrationFrom13To14::class),
        AutoMigration(from = 14, to = 15, spec = HistoryDatabase.MigrationFrom14To15::class),
    ]
)
@TypeConverters(com.neeva.app.storage.TypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): HistoryDao
    abstract fun spaceDao(): SpaceDao
    abstract fun hostInfoDao(): HostInfoDao

    companion object {
        private val TAG = HistoryDatabase::class.simpleName
        private const val DATABASE_FILENAME = "HistoryDB"
        private const val CACHE_IMPORT_PATH = "extracted"
        private const val CHECK_FOR_IMPORTED_DATABASE_KEY = "CHECK_FOR_IMPORTED_DATABASE_KEY"

        fun create(
            context: Context,
            sharedPreferencesModel: SharedPreferencesModel
        ): HistoryDatabase {
            importDatabaseIfNecessary(context, sharedPreferencesModel)

            return Room
                .databaseBuilder(context, HistoryDatabase::class.java, DATABASE_FILENAME)
                .fallbackToDestructiveMigration()
                .build()
        }

        internal fun createInMemory(context: Context): HistoryDatabase {
            return Room
                .inMemoryDatabaseBuilder(context, HistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }

        /** Extracts a zip file into a location that will be checked on startup. */
        @WorkerThread
        fun prepareDatabaseForImport(
            context: Context,
            contentUri: Uri,
            sharedPreferencesModel: SharedPreferencesModel
        ) {
            sharedPreferencesModel.setValue(
                SharedPrefFolder.APP,
                CHECK_FOR_IMPORTED_DATABASE_KEY,
                true
            )

            if (ZipUtils.extract(context, contentUri, File(context.cacheDir, CACHE_IMPORT_PATH))) {
                // Kill the process so that Room won't complain about the database being in a bad
                // state when its files are overwritten.
                Process.killProcess(Process.myPid())
            }
        }

        /**
         * Checks to see if there is a database that is waiting to be imported.
         *
         * This deletes the user's existing database and copies over a database that was previously
         * extracted.  It's definitely not the best way to do this, but is handy for debugging.
         */
        private fun importDatabaseIfNecessary(
            context: Context,
            sharedPreferencesModel: SharedPreferencesModel
        ) {
            val checkForDatabase = sharedPreferencesModel.getValue(
                SharedPrefFolder.APP,
                CHECK_FOR_IMPORTED_DATABASE_KEY,
                false
            )
            if (!checkForDatabase) return
            sharedPreferencesModel.setValue(
                SharedPrefFolder.APP,
                CHECK_FOR_IMPORTED_DATABASE_KEY,
                false
            )

            val extractedDirectory = context.cacheDir.resolve(CACHE_IMPORT_PATH)
            val extractedDatabaseDirectory = extractedDirectory.resolve("databases")
            try {
                // Purposefully done on the main thread because we need to block on having the
                // database ready to use.
                if (extractedDatabaseDirectory.exists()) {
                    Log.d(TAG, "Detected database to be imported.")

                    // Delete the old database files.
                    val databaseDirectory = File(context.dataDir, "databases")
                    databaseDirectory.listFiles()?.forEach { file ->
                        file.delete()
                    }

                    // Copy over the new database into the database directory.
                    extractedDatabaseDirectory.listFiles()?.forEach { file ->
                        val renamedFile = File(databaseDirectory, file.name)
                        file.renameTo(renamedFile)
                        Log.d(TAG, "Moved ${file.path} to ${renamedFile.path}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed during import of existing database", e)
            } finally {
                extractedDirectory.deleteRecursively()
            }
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

    class MigrationFrom12To13 : AutoMigrationSpec

    class MigrationFrom13To14 : AutoMigrationSpec

    @DeleteColumn.Entries(
        DeleteColumn(tableName = "SpaceItem", columnName = "entityType")
    )
    class MigrationFrom14To15 : AutoMigrationSpec

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
