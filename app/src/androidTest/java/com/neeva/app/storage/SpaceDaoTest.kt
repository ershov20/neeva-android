package com.neeva.app.storage

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.storage.daos.SpaceDao
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.type.SpaceACLLevel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class SpaceDaoTest : HistoryDatabaseBaseTest() {
    private lateinit var spacesRepository: SpaceDao

    override fun setUp() {
        super.setUp()
        spacesRepository = database.spaceDao()
    }

    private suspend fun addSpacesIntoDatabase() {
        // Setup: Add entries into the database.
        spacesRepository.upsert(SPACE_1)
        spacesRepository.upsert(SPACE_2)

        spacesRepository.upsert(
            SpaceItem(
                "asjdahjfad",
                "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                Uri.parse("https://example.com"),
                "Example",
                null,
                null
            )
        )
        spacesRepository.upsert(
            SpaceItem(
                "ksjkjkdadkma",
                "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                Uri.parse("https://android.com"),
                "Android",
                null,
                null
            )
        )
        spacesRepository.upsert(
            SpaceItem(
                "sdsfgaskals",
                "c5rgtmtdv9enb8j1gv60",
                Uri.parse("https://android.com"),
                "Android",
                null,
                null
            )
        )
    }

    @Test
    fun insert() {
        runBlocking {
            addSpacesIntoDatabase()
            val spaces = database.spaceDao().allSpaces()

            expectThat(spaces).hasSize(2)
            expectThat(spaces[0]).isEqualTo(SPACE_1)
            expectThat(spaces[1]).isEqualTo(SPACE_2)

            val space1Items = database.spaceDao().getItemsFromSpace(SPACE_1.id)
            expectThat(space1Items).hasSize(SPACE_1.resultCount)

            val space2Items = database.spaceDao().getItemsFromSpace(SPACE_2.id)
            expectThat(space2Items).hasSize(SPACE_2.resultCount)
        }
    }

    @Test
    fun getSpaceIDsWithURL() {
        runBlocking {
            addSpacesIntoDatabase()

            val spaceIDs = database.spaceDao()
                .getSpaceIDsWithURL(Uri.parse("https://android.com"))
            expectThat(spaceIDs).hasSize(2)
            expectThat(spaceIDs).contains(SPACE_1.id, SPACE_2.id)
        }
    }

    @Test
    fun deleteOrphanedSpaceEntities() {
        runBlocking {
            addSpacesIntoDatabase()
            val spacesBefore = database.spaceDao().allSpaces()
            expectThat(spacesBefore).hasSize(2)

            // Delete one of the Spaces.
            spacesRepository.deleteSpace(SPACE_2)
            val spacesAfter = database.spaceDao().allSpaces()
            expectThat(spacesAfter).hasSize(1)

            // At this point the space items should still be inside the database.
            var space2Items = database.spaceDao().getItemsFromSpace(SPACE_2.id)
            expectThat(space2Items).hasSize(SPACE_2.resultCount)

            database.spaceDao().deleteOrphanedSpaceItems()

            // At this point the space items should not be inside the database.
            space2Items = database.spaceDao().getItemsFromSpace(SPACE_2.id)
            expectThat(space2Items).isEmpty()
        }
    }

    companion object {
        private val SPACE_1 = Space(
            id = "c5rgtmtdv9enb8j1gv60",
            name = "Saved For Later",
            lastModifiedTs = "2022-02-10T22:08:01Z",
            thumbnail = null,
            resultCount = 1,
            isDefaultSpace = true,
            isShared = true,
            isPublic = false,
            userACL = SpaceACLLevel.Owner
        )
        private val SPACE_2 = Space(
            id = "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
            name = "Jetpack Compose",
            lastModifiedTs = "2022-02-10T02:10:38Z",
            thumbnail = null,
            resultCount = 2,
            isDefaultSpace = false,
            isShared = false,
            isPublic = true,
            userACL = SpaceACLLevel.PublicView
        )
    }
}
