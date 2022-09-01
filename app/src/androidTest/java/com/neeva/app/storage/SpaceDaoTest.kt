// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage

import android.net.Uri
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.spaces.toSpace
import com.neeva.app.storage.daos.SpaceDao
import com.neeva.app.storage.entities.SpaceEntityType
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.storage.entities.spaceItem
import com.neeva.testcommon.apollo.MockListSpacesQueryData
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@HiltAndroidTest
class SpaceDaoTest : HistoryDatabaseBaseTest() {
    companion object {
        private val SPACE_1 = MockListSpacesQueryData.SPACE_1.toSpace("user id 1")!!
        private val SPACE_2 = MockListSpacesQueryData.SPACE_2.toSpace("user id 2")!!
        private val SPACE_1_ITEM_1 = MockListSpacesQueryData.SPACE_1_ITEM_1.spaceItem(SPACE_1.id)!!
        private val SPACE_2_ITEM_1 = MockListSpacesQueryData.SPACE_2_ITEM_1.spaceItem(SPACE_2.id)!!
        private val SPACE_2_ITEM_2 = MockListSpacesQueryData.SPACE_2_ITEM_2.spaceItem(SPACE_2.id)!!
    }

    private lateinit var spacesRepository: SpaceDao

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    override fun setUp() {
        super.setUp()
        spacesRepository = database.spaceDao()
    }

    private suspend fun addSpacesIntoDatabase() {
        // Setup: Add entries into the database.
        spacesRepository.upsert(SPACE_1)
        spacesRepository.upsert(SPACE_2)
        spacesRepository.upsert(SPACE_1_ITEM_1)
        spacesRepository.upsert(SPACE_2_ITEM_1)
        spacesRepository.upsert(SPACE_2_ITEM_2)
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

            database.spaceDao()
                .getSpaceIDsWithURL(Uri.parse("https://android.com"))
                .apply { expectThat(this).isEmpty() }

            // Insert the item into one of the Spaces.
            spacesRepository.upsert(
                SpaceItem(
                    "first instance",
                    SPACE_2.id,
                    Uri.parse("https://android.com"),
                    "Android",
                    null,
                    null,
                    itemIndex = 0
                )
            )

            database.spaceDao()
                .getSpaceIDsWithURL(Uri.parse("https://android.com"))
                .apply { expectThat(this).containsExactly(SPACE_2.id) }

            // Insert an item with the same URL into another Space.
            spacesRepository.upsert(
                SpaceItem(
                    "second instance",
                    SPACE_1.id,
                    Uri.parse("https://android.com"),
                    "Android",
                    null,
                    null,
                    itemIndex = 0
                )
            )

            database.spaceDao()
                .getSpaceIDsWithURL(Uri.parse("https://android.com"))
                .apply { expectThat(this).containsExactlyInAnyOrder(SPACE_1.id, SPACE_2.id) }
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

    @Test
    fun insertRichEntities() {
        runBlocking {
            addSpacesIntoDatabase()
            val spaces = database.spaceDao().allSpaces()

            expectThat(spaces).hasSize(2)
            expectThat(spaces[0]).isEqualTo(SPACE_1)
            expectThat(spaces[1]).isEqualTo(SPACE_2)

            var space1Items = database.spaceDao().getItemsFromSpace(SPACE_1.id)
            expectThat(space1Items).hasSize(SPACE_1.resultCount)

            val space2Items = database.spaceDao().getItemsFromSpace(SPACE_2.id)
            expectThat(space2Items).hasSize(SPACE_2.resultCount)

            val spaceItem = GetSpacesDataQuery.Entity(
                GetSpacesDataQuery.Metadata(
                    docID = "abc"
                ),
                GetSpacesDataQuery.SpaceEntity(
                    "https://news.com",
                    "",
                    "",
                    "",
                    GetSpacesDataQuery.Content(
                        "xyz",
                        title = null,
                        snippet = null,
                        __typename = "type",
                        typeSpecific = GetSpacesDataQuery.TypeSpecific(
                            __typename = "type",
                            onTechDoc = null,
                            onNewsItem = GetSpacesDataQuery.OnNewsItem(
                                GetSpacesDataQuery.NewsItem(
                                    title = "Big news",
                                    snippet = "You can add news to your Spaces",
                                    url = "https://news.com",
                                    thumbnailImage = GetSpacesDataQuery.ThumbnailImage(
                                        "https://news.com/thumbnails/0"
                                    ),
                                    providerName = "News",
                                    datePublished = "30 Nov",
                                    favIconURL = "https://news.com/favicon",
                                    preference = null,
                                    domain = "news.com"
                                )
                            ),
                            onWeb = null,
                            onRichEntity = null
                        ),
                        actionURL = ""
                    )
                )
            ).spaceItem(SPACE_1.id, null)
            spaceItem?.itemIndex = 2
            expectThat(spaceItem).isNotNull()
            spacesRepository.upsert(spaceItem!!)

            space1Items = database.spaceDao().getItemsFromSpace(SPACE_1.id)
            expectThat(space1Items.last().id).isEqualTo("abc")
            expectThat(space1Items.last().itemEntityType).isEqualTo(SpaceEntityType.NEWS)
            expectThat(space1Items.last().title).isEqualTo("Big news")
            expectThat(space1Items.last().snippet).isEqualTo("You can add news to your Spaces")
            expectThat(space1Items.last().url).isEqualTo(Uri.parse("https://news.com"))
            expectThat(space1Items.last().faviconURL).isEqualTo(
                Uri.parse("https://news.com/favicon")
            )
            expectThat(space1Items.last().provider).isEqualTo("News")
            expectThat(space1Items.last().datePublished).isEqualTo("30 Nov")
            expectThat(space1Items.last().thumbnail).isEqualTo(
                Uri.parse("https://news.com/thumbnails/0")
            )

            val spaceItem2 = GetSpacesDataQuery.Entity(
                GetSpacesDataQuery.Metadata(
                    docID = "def"
                ),
                GetSpacesDataQuery.SpaceEntity(
                    "https://recipe.com",
                    "",
                    "Personal comments on recipe",
                    "",
                    GetSpacesDataQuery.Content(
                        "def2",
                        title = null,
                        snippet = null,
                        __typename = "type",
                        typeSpecific = GetSpacesDataQuery.TypeSpecific(
                            __typename = "type",
                            onTechDoc = null,
                            onNewsItem = null,
                            onWeb = GetSpacesDataQuery.OnWeb(
                                GetSpacesDataQuery.Web(
                                    recipes = listOf(
                                        GetSpacesDataQuery.Recipe(
                                            title = "Recipe",
                                            imageURL = "https://recipe.com/thumbnails/0",
                                            source = "",
                                            totalTime = "1 hrs 23 mins",
                                            recipeRating = GetSpacesDataQuery.RecipeRating(
                                                recipeStars = 4.3,
                                                numReviews = 123
                                            )
                                        )
                                    ),
                                    retailerProduct = null
                                )
                            ),
                            onRichEntity = null
                        ),
                        actionURL = ""
                    )
                )
            ).spaceItem(SPACE_1.id, null)
            spaceItem2?.itemIndex = 3
            expectThat(spaceItem2).isNotNull()
            spacesRepository.upsert(spaceItem2!!)

            // This should have registered as a recipe. We should have preferred personal comments
            // over comments from reviews

            space1Items = database.spaceDao().getItemsFromSpace(SPACE_1.id)
            expectThat(space1Items.last().id).isEqualTo("def")
            expectThat(space1Items.last().itemEntityType).isEqualTo(SpaceEntityType.RECIPE)
            expectThat(space1Items.last().title).isEqualTo("Recipe")
            expectThat(space1Items.last().snippet).isEqualTo("Personal comments on recipe")
            expectThat(space1Items.last().url).isEqualTo(Uri.parse("https://recipe.com"))
            expectThat(space1Items.last().faviconURL).isEqualTo(null)
            expectThat(space1Items.last().stars).isEqualTo(4.3)
            expectThat(space1Items.last().numReviews).isEqualTo(123)
            expectThat(space1Items.last().totalTime).isEqualTo("1 hrs 23 mins")

            val spaceItem3 = GetSpacesDataQuery.Entity(
                GetSpacesDataQuery.Metadata(
                    docID = "ghi"
                ),
                GetSpacesDataQuery.SpaceEntity(
                    "https://product.com",
                    "",
                    "Personal comments on the product",
                    "",
                    GetSpacesDataQuery.Content(
                        "ghi2",
                        title = null,
                        snippet = null,
                        __typename = "type",
                        typeSpecific = GetSpacesDataQuery.TypeSpecific(
                            __typename = "type",
                            onTechDoc = null,
                            onNewsItem = null,
                            onWeb = GetSpacesDataQuery.OnWeb(
                                GetSpacesDataQuery.Web(
                                    recipes = null,
                                    retailerProduct = GetSpacesDataQuery.RetailerProduct(
                                        url = "https://product.com",
                                        name = "Product title",
                                        description = listOf("Product description"),
                                        priceHistory = GetSpacesDataQuery.PriceHistory(
                                            currentPrice = 123.45
                                        ),
                                        reviews = GetSpacesDataQuery.Reviews(
                                            GetSpacesDataQuery.RatingSummary(
                                                numReviews = 145,
                                                rating = GetSpacesDataQuery.Rating(
                                                    productStars = 2.4
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            onRichEntity = null
                        ),
                        actionURL = ""
                    )
                )
            ).spaceItem(SPACE_1.id, null)
            spaceItem3?.itemIndex = 4
            expectThat(spaceItem3).isNotNull()
            spacesRepository.upsert(spaceItem3!!)

            // This should have registered as a product. We should have preferred personal comments
            // over comments from reviews

            space1Items = database.spaceDao().getItemsFromSpace(SPACE_1.id)
            expectThat(space1Items.last().id).isEqualTo("ghi")
            expectThat(space1Items.last().itemEntityType).isEqualTo(SpaceEntityType.PRODUCT)
            expectThat(space1Items.last().title).isEqualTo("Product title")
            expectThat(space1Items.last().snippet).isEqualTo("Personal comments on the product")
            expectThat(space1Items.last().url).isEqualTo(Uri.parse("https://product.com"))
            expectThat(space1Items.last().stars).isEqualTo(2.4)
            expectThat(space1Items.last().numReviews).isEqualTo(145)
            expectThat(space1Items.last().price).isEqualTo(123.45)
        }
    }
}
