package com.neeva.app.storage

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec
import com.neeva.app.storage.entities.HostInfo

object Migrations {
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

    /**
     * The [HostInfo] primary key was changed to be the hostname rather than a monotonically
     * increasing integer.  Because we had unit tests on that table, we can be fairly certain that
     * dropping the column without doing a more complex migration will work.
     */
    @DeleteColumn.Entries(
        DeleteColumn(tableName = "HostInfo", columnName = "hostUID")
    )
    class MigrationFrom15To16 : AutoMigrationSpec

    class MigrationFrom16To17 : AutoMigrationSpec
}
