package com.neeva.app.spaces

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.apollographql.apollo3.api.Optional
import com.neeva.app.AddToSpaceMutation
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.DeleteSpaceResultByURLMutation
import com.neeva.app.Dispatchers
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.TestAuthenticatedApolloWrapper
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.Directories
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData
import com.neeva.app.userdata.NeevaUserToken
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceStoreTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock
    private lateinit var popupModel: PopupModel

    private lateinit var context: Context
    private lateinit var neevaUser: NeevaUser
    private lateinit var database: HistoryDatabase
    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper
    private lateinit var spaceStore: SpaceStore
    private lateinit var file: File
    private lateinit var dispatchers: Dispatchers
    private lateinit var neevaConstants: NeevaConstants

    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()

        context = ApplicationProvider.getApplicationContext()
        database = HistoryDatabase.createInMemory(context)
        val neevaUserToken = NeevaUserToken(
            sharedPreferencesModel = SharedPreferencesModel(context),
            neevaConstants = neevaConstants
        )
        neevaUserToken.setToken("NotAnEmptyToken")

        neevaUser = NeevaUser(
            data = NeevaUserData("c5rgtdldv9enb8j1gupg"),
            neevaUserToken = neevaUserToken
        )
        apolloWrapper = TestAuthenticatedApolloWrapper(
            neevaUserToken = neevaUserToken,
            neevaConstants = neevaConstants
        )
        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )
        spaceStore = SpaceStore(
            appContext = context,
            historyDatabase = database,
            coroutineScope = coroutineScopeRule.scope,
            unauthenticatedApolloWrapper = mock(),
            authenticatedApolloWrapper = apolloWrapper,
            neevaUser = neevaUser,
            neevaConstants = neevaConstants,
            popupModel = popupModel,
            dispatchers = dispatchers,
            directories = Directories(
                context = context,
                coroutineScope = coroutineScopeRule.scope,
                dispatchers = dispatchers
            )
        )
        file = context.cacheDir.resolve("space_store_test")
    }

    @Test fun convertApolloSpace_givenAllRequiredData_createsSpace() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            val original = SPACE_1
            original.toSpace(neevaUser.data.id)!!.apply {
                expectThat(id).isEqualTo("c5rgtmtdv9enb8j1gv60")
                expectThat(name).isEqualTo("Saved For Later")
                expectThat(lastModifiedTs).isEqualTo("2022-02-10T22:08:01Z")
                expectThat(thumbnail).isEqualTo(null)
                expectThat(resultCount).isEqualTo(0)
                expectThat(isDefaultSpace).isTrue()
                expectThat(userACL).isEqualTo(SpaceACLLevel.Owner)
                expectThat(isPublic).isTrue()
                expectThat(isShared).isTrue()
            }
        }

    @Test fun convertApolloSpace_properlyHandlesAclInfo() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            neevaUser.data = NeevaUserData(id = "wrong id")
            val original = SPACE_2.copy(
                space = SPACE_2.space!!.copy(hasPublicACL = false)
            )
            original.toSpace(neevaUser.data.id)!!.apply {
                expectThat(userACL).isEqualTo(SpaceACLLevel.Comment)
                expectThat(isPublic).isFalse()
                expectThat(isShared).isFalse()
            }
        }

    @Test
    fun refresh_spaceThumbnailsWrittenToDiskAndCleanedUpOnce() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            val oldDirectory = File(spaceStore.spacesDirectory.await(), "oldspaceID")
            oldDirectory.mkdirs()

            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            expectThat(oldDirectory.exists()).isTrue()

            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(SPACE_1.pageMetadata!!.pageID!!, SPACE_2.pageMetadata!!.pageID!!)
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            val spaceID = SPACE_1.pageMetadata!!.pageID!!
            val directory = File(spaceStore.spacesDirectory.await(), spaceID)
            val file = File(directory, spaceID)
            expectThat(directory.exists()).isTrue()
            expectThat(file.exists()).isTrue()

            // Confirm old directory is gone.
            expectThat(oldDirectory.exists()).isFalse()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            apolloWrapper.registerTestResponse(
                ListSpacesQuery(),
                RESPONSE_LIST_SPACE_QUERY_WITH_FIRST_SPACE_DELETED_AND_SECOND_UPDATED
            )
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(SPACE_1.pageMetadata!!.pageID!!, SPACE_2.pageMetadata!!.pageID!!)
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            // Confirm SPACE_1 directory remains because the cleanup only runs once.
            expectThat(directory.exists()).isTrue()
            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(4)
                expectThat(get(2)).isA<ListSpacesQuery>()
                expectThat(get(3)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun refresh_schedulesAnExtraRefresh() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(
                            SPACE_1.pageMetadata!!.pageID!!,
                            SPACE_2.pageMetadata!!.pageID!!
                        )
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY
            )
            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)

            spaceStore.refresh()
            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(3)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
                expectThat(get(2)).isA<ListSpacesQuery>()
            }
        }

    @Test
    fun refresh_skipsWhileLoggedOut() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            neevaUser.neevaUserToken.setToken("")

            expectThat(neevaUser.neevaUserToken.getToken()).isEmpty()

            spaceStore.refresh()

            expectThat(apolloWrapper.testApolloClientWrapper.performedOperations).isEmpty()
        }

    @Test fun convertApolloSpace_withMissingData_returnsNull() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            expectThat(
                SPACE_1.copy(pageMetadata = null)
                    .toSpace(neevaUser.data.id)
            ).isNull()

            expectThat(
                SPACE_1.copy(space = null)
                    .toSpace(neevaUser.data.id)
            ).isNull()

            expectThat(
                SPACE_1.copy(space = SPACE_1.space!!.copy(name = null))
                    .toSpace(neevaUser.data.id)
            ).isNull()

            expectThat(
                SPACE_1
                    .copy(space = SPACE_1.space!!.copy(lastModifiedTs = null))
                    .toSpace(neevaUser.data.id)
            ).isNull()

            expectThat(
                SPACE_1.copy(space = SPACE_1.space!!.copy(userACL = null))
                    .toSpace(neevaUser.data.id)
            ).isNull()
        }

    @Test
    fun refresh_withValidSpaceListQueryResponse_returnsBothSpaces() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            // Only allow the response to fetch the user's Spaces to succeed.
            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            // The refresh will get data about the Spaces but not their contents.
            val allSpaces = database.spaceDao().allSpaces()
            expectThat(allSpaces).hasSize(2)
            allSpaces[0].apply {
                expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
            }
            allSpaces[1].apply {
                expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
            }

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun refresh_withValidResponses_returnsAllData() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(
                            SPACE_1.pageMetadata!!.pageID!!,
                            SPACE_2.pageMetadata!!.pageID!!
                        )
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
                }
                allSpaces[1].apply {
                    expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                    expectThat(
                        database.spaceDao()
                            .getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://developer.android.com/jetpack/compose/")
                    )
                }
            }
            database.spaceDao().allSpaces()
                .filterNot { it.userACL >= SpaceACLLevel.Edit }
                .let { editableSpaces ->
                    expectThat(editableSpaces).hasSize(1)
                    editableSpaces[0].apply {
                        expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                        expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
                    }
                }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun refresh_updatesWhenSpaceTimestampChanges() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(
                            SPACE_1.pageMetadata!!.pageID!!,
                            SPACE_2.pageMetadata!!.pageID!!
                        )
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
                }
                allSpaces[1].apply {
                    expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs).isEqualTo("2022-02-10T02:10:38Z")
                    expectThat(
                        database.spaceDao()
                            .getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://developer.android.com/jetpack/compose/")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            apolloWrapper.registerTestResponse(
                ListSpacesQuery(),
                RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED
            )
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(SPACE_2.pageMetadata!!.pageID!!)
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY
            )

            spaceStore.refresh()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
                }
                allSpaces[1].apply {
                    expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs).isEqualTo("2099-02-10T02:12:38Z")
                    expectThat(
                        database.spaceDao().getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://reddit.com/r/android")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isFalse()
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://reddit.com/r/android")
                )
            ).isTrue()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(4)
                expectThat(get(2)).isA<ListSpacesQuery>()
                expectThat(get(3)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun addToSpace_mutatesAndUpdatesLocalStateOnly() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(
                            SPACE_1.pageMetadata!!.pageID!!,
                            SPACE_2.pageMetadata!!.pageID!!
                        )
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
                }
                allSpaces[1].apply {
                    expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs).isEqualTo("2022-02-10T02:10:38Z")
                    expectThat(
                        database.spaceDao().getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://developer.android.com/jetpack/compose/")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            apolloWrapper.registerTestResponse(
                spaceStore.createAddToSpaceMutation(
                    space = SPACE_2.toSpace(neevaUser.data.id)!!,
                    url = Uri.parse("https://example.com"),
                    title = "Example page"
                ),
                RESPONSE_ADD_TO_SPACE_MUTATION
            )

            val success = spaceStore.addOrRemoveFromSpace(
                SPACE_2.pageMetadata?.pageID!!,
                Uri.parse("https://example.com"),
                "Example page"
            )

            expectThat(success).isTrue()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(3)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
                expectThat(get(2)).isA<AddToSpaceMutation>()
            }

            expectThat(spaceStore.spaceStoreContainsUrl(Uri.parse("https://example.com"))).isTrue()
        }

    @Test
    fun deleteFromSpace_mutatesAndUpdatesLocalStateOnly() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.registerTestResponse(ListSpacesQuery(), RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(
                            SPACE_1.pageMetadata!!.pageID!!,
                            SPACE_2.pageMetadata!!.pageID!!
                        )
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
                }
                allSpaces[1].apply {
                    expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs).isEqualTo("2022-02-10T02:10:38Z")
                    expectThat(
                        database.spaceDao()
                            .getItemsFromSpace(id)
                            .map { it.url }
                    )
                        .containsExactly(
                            Uri.parse("https://developer.android.com/jetpack/compose/")
                        )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            val urlToRemove = database.spaceDao()
                .getItemsFromSpace(SPACE_2.pageMetadata?.pageID!!)
                .first()
                .url!!
            apolloWrapper.registerTestResponse(
                spaceStore.createDeleteSpaceResultByURLMutation(
                    SPACE_2.toSpace(neevaUser.data.id)!!,
                    urlToRemove
                ),
                RESPONSE_DELETE_FROM_SPACE_MUTATION
            )

            val success = spaceStore.addOrRemoveFromSpace(
                spaceID = SPACE_2.pageMetadata?.pageID!!,
                url = urlToRemove,
                title = ""
            )

            expectThat(success).isTrue()

            apolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(3)
                expectThat(get(2)).isA<DeleteSpaceResultByURLMutation>()
            }

            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isFalse()
        }

    companion object {
        private val SPACE_1 = ListSpacesQuery.Space(
            pageMetadata = ListSpacesQuery.PageMetadata(pageID = "c5rgtmtdv9enb8j1gv60"),
            stats = null,
            space = ListSpacesQuery.Space1(
                name = "Saved For Later",
                description = null,
                lastModifiedTs = "2022-02-10T22:08:01Z",
                userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Owner),
                acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
                hasPublicACL = true,
                resultCount = null,
                isDefaultSpace = true,
                owner = null,
            )
        )

        private val SPACE_2 = ListSpacesQuery.Space(
            pageMetadata = ListSpacesQuery.PageMetadata(
                pageID = "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
            ),
            stats = null,
            space = ListSpacesQuery.Space1(
                name = "Jetpack Compose",
                description = null,
                lastModifiedTs = "2022-02-10T02:10:38Z",
                userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Comment),
                acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
                hasPublicACL = false,
                resultCount = 1,
                isDefaultSpace = false,
                owner = null
            )
        )

        private val RESPONSE_LIST_SPACE_QUERY = ListSpacesQuery.Data(
            listSpaces = ListSpacesQuery.ListSpaces(
                requestID = "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
                space = listOf(SPACE_1, SPACE_2)
            )
        )

        private val RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED = ListSpacesQuery.Data(
            listSpaces = ListSpacesQuery.ListSpaces(
                requestID = "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
                space = listOf(
                    SPACE_1,
                    SPACE_2.copy(
                        space = SPACE_2.space!!.copy(
                            lastModifiedTs = "2099-02-10T02:12:38Z"
                        )
                    )
                )
            )
        )

        private val RESPONSE_LIST_SPACE_QUERY_WITH_FIRST_SPACE_DELETED_AND_SECOND_UPDATED =
            ListSpacesQuery.Data(
                listSpaces = ListSpacesQuery.ListSpaces(
                    requestID = "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
                    space = listOf(
                        SPACE_2.copy(
                            space = SPACE_2.space!!.copy(
                                lastModifiedTs = "2099-02-10T02:12:38Z"
                            )
                        )
                    )
                )
            )

        private val RESPONSE_GET_SPACES_DATA_QUERY = GetSpacesDataQuery.Data(
            getSpace = GetSpacesDataQuery.GetSpace(
                space = listOf(
                    GetSpacesDataQuery.Space(
                        pageMetadata = GetSpacesDataQuery.PageMetadata(
                            pageID = "c5rgtmtdv9enb8j1gv60"
                        ),
                        space = GetSpacesDataQuery.Space1(
                            thumbnail = "data:image/jpeg;base64,still garbage",
                            entities = emptyList(),
                            description = null,
                            name = null,
                            owner = null
                        ),
                        stats = null
                    ),
                    GetSpacesDataQuery.Space(
                        pageMetadata = GetSpacesDataQuery.PageMetadata(
                            pageID = "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                        ),
                        space = GetSpacesDataQuery.Space1(
                            thumbnail = "data:image/jpeg;base64,still garbage",
                            entities = listOf(
                                GetSpacesDataQuery.Entity(
                                    metadata = GetSpacesDataQuery.Metadata(
                                        docID = "skadksnflkaamda345"
                                    ),
                                    spaceEntity = GetSpacesDataQuery.SpaceEntity(
                                        url = "https://developer.android.com/jetpack/compose/",
                                        title = "Testing your Compose layout | Jetpack Compose",
                                        snippet = null,
                                        thumbnail = "data:image/jpeg;base64,garbage",
                                        content = null
                                    )
                                )
                            ),
                            description = null,
                            name = null,
                            owner = null
                        ),
                        stats = null
                    )
                )
            )
        )

        private val RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY = GetSpacesDataQuery.Data(
            getSpace = GetSpacesDataQuery.GetSpace(
                space = listOf(
                    GetSpacesDataQuery.Space(
                        pageMetadata = GetSpacesDataQuery.PageMetadata(
                            pageID = "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                        ),
                        space = GetSpacesDataQuery.Space1(
                            thumbnail = "data:image/jpeg;base64,different garbage",
                            entities = listOf(
                                GetSpacesDataQuery.Entity(
                                    metadata = GetSpacesDataQuery.Metadata(
                                        docID = "skadksnflkaamda345"
                                    ),
                                    spaceEntity = GetSpacesDataQuery.SpaceEntity(
                                        url = "https://reddit.com/r/android",
                                        title = "Android subreddit",
                                        snippet = null,
                                        thumbnail = "data:image/jpeg;base64,also garbage",
                                        content = null
                                    )
                                )
                            ),
                            description = null,
                            name = null,
                            owner = null
                        ),
                        stats = null
                    )
                )
            )
        )

        private val RESPONSE_ADD_TO_SPACE_MUTATION = AddToSpaceMutation.Data(
            entityId = "nEgvD5HST7e62eEmfg0kkxx4xnEuNHBeEXxbGcoo"
        )

        private val RESPONSE_DELETE_FROM_SPACE_MUTATION = DeleteSpaceResultByURLMutation.Data(
            deleteSpaceResultByURL = true
        )
    }
}
