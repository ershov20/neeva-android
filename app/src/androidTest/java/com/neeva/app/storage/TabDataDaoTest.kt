package com.neeva.app.storage

import android.net.Uri
import com.neeva.app.storage.daos.TabDataDao
import com.neeva.app.storage.entities.TabData
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@HiltAndroidTest
class TabDataDaoTest : HistoryDatabaseBaseTest() {
    lateinit var tabDataDao: TabDataDao

    override fun setUp() {
        super.setUp()
        tabDataDao = database.tabDataDao()
    }

    @Test
    fun addAndReplace() {
        expectThat(tabDataDao.getAll()).isEmpty()
        tabDataDao.add(FIRST_ENTRY)

        expectThat(tabDataDao.getAll()).containsExactly(FIRST_ENTRY)
        tabDataDao.add(SECOND_ENTRY)

        expectThat(tabDataDao.getAll()).containsExactlyInAnyOrder(FIRST_ENTRY, SECOND_ENTRY)
        expectThat(tabDataDao.getAllArchived()).containsExactlyInAnyOrder(FIRST_ENTRY, SECOND_ENTRY)

        // Conflicts result in overwriting an existing entry.
        tabDataDao.add(FIRST_ENTRY_REPLACED)
        expectThat(tabDataDao.getAll())
            .containsExactlyInAnyOrder(FIRST_ENTRY_REPLACED, SECOND_ENTRY)
        expectThat(tabDataDao.getAllArchived()).containsExactlyInAnyOrder(SECOND_ENTRY)
    }

    @Test
    fun get() {
        expectThat(tabDataDao.getAll()).isEmpty()

        tabDataDao.add(FIRST_ENTRY)
        tabDataDao.add(SECOND_ENTRY)
        expectThat(tabDataDao.get("id 1")).isEqualTo(FIRST_ENTRY)
        expectThat(tabDataDao.get("id 2")).isEqualTo(SECOND_ENTRY)

        // Conflicts result in overwriting an existing entry.
        tabDataDao.add(FIRST_ENTRY_REPLACED)
        expectThat(tabDataDao.get("id 1")).isEqualTo(FIRST_ENTRY_REPLACED)
        expectThat(tabDataDao.get("id 2")).isEqualTo(SECOND_ENTRY)
    }

    @Test
    fun delete() {
        expectThat(tabDataDao.getAll()).isEmpty()

        tabDataDao.add(FIRST_ENTRY)
        tabDataDao.add(SECOND_ENTRY)
        tabDataDao.add(THIRD_ENTRY)
        expectThat(tabDataDao.getAll())
            .containsExactlyInAnyOrder(FIRST_ENTRY, SECOND_ENTRY, THIRD_ENTRY)

        tabDataDao.delete(SECOND_ENTRY.id)
        expectThat(tabDataDao.getAll()).containsExactlyInAnyOrder(FIRST_ENTRY, THIRD_ENTRY)
    }

    @Test
    fun deleteAllArchived() {
        expectThat(tabDataDao.getAll()).isEmpty()

        tabDataDao.add(FIRST_ENTRY)
        tabDataDao.add(SECOND_ENTRY)
        tabDataDao.add(THIRD_ENTRY)
        expectThat(tabDataDao.getAll())
            .containsExactlyInAnyOrder(FIRST_ENTRY, SECOND_ENTRY, THIRD_ENTRY)

        tabDataDao.deleteAllArchived()
        expectThat(tabDataDao.getAll()).containsExactly(THIRD_ENTRY)
    }

    companion object {
        val FIRST_ENTRY = TabData(
            id = "id 1",
            url = Uri.parse("https://www.example.com"),
            title = "Example",
            lastActiveMs = 10000L,
            isArchived = true
        )
        val SECOND_ENTRY = TabData(
            id = "id 2",
            url = Uri.parse("https://www.neeva.com"),
            title = "Neeva",
            lastActiveMs = 30000L,
            isArchived = true
        )
        val THIRD_ENTRY = TabData(
            id = "id 3",
            url = Uri.parse("https://www.third.com"),
            title = "Third",
            lastActiveMs = 50000L,
            isArchived = false
        )
        val FIRST_ENTRY_REPLACED = TabData(
            id = "id 1",
            url = Uri.parse("https://www.replacement.com"),
            title = "Replacement",
            lastActiveMs = 20000L,
            isArchived = false
        )
    }
}
