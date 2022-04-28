package com.neeva.app.spaces

import android.content.Context
import android.database.sqlite.SQLiteDatabase.createInMemory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.AddToSpaceMutation
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.DeleteSpaceResultByURLMutation
import com.neeva.app.Dispatchers
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.TestApolloWrapper
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.SnackbarModel
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
    private lateinit var snackbarModel: SnackbarModel

    private lateinit var context: Context
    private lateinit var neevaUser: NeevaUser
    private lateinit var database: HistoryDatabase
    private lateinit var apolloWrapper: TestApolloWrapper
    private lateinit var spaceStore: SpaceStore
    private lateinit var file: File
    private lateinit var testDispatcher: Dispatchers

    override fun setUp() {
        super.setUp()

        context = ApplicationProvider.getApplicationContext()
        database = HistoryDatabase.createInMemory(context)
        val neevaUserToken = NeevaUserToken(mock())
        neevaUserToken.setToken("NotAnEmptyToken")

        neevaUser = NeevaUser(
            data = NeevaUserData("c5rgtdldv9enb8j1gupg"),
            neevaUserToken = neevaUserToken
        )
        apolloWrapper = TestApolloWrapper(neevaUserToken = neevaUserToken)
        testDispatcher = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )
        spaceStore = SpaceStore(
            context,
            database,
            coroutineScopeRule.scope,
            mock(),
            apolloWrapper,
            neevaUser,
            snackbarModel,
            testDispatcher
        )
        file = context.cacheDir.resolve("space_store_test")
    }

    override fun tearDown() {
        apolloWrapper.tearDown()
        super.tearDown()
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
            val oldDirectory = File(spaceStore.thumbnailDirectory, "oldspaceID")
            oldDirectory.mkdirs()

            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            expectThat(oldDirectory.exists()).isTrue()

            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            val spaceID = SPACE_1.pageMetadata!!.pageID
            val directory = File(spaceStore.thumbnailDirectory, spaceID)
            val file = File(directory, spaceID)
            expectThat(directory.exists()).isTrue()
            expectThat(file.exists()).isTrue()

            // Confirm old directory is gone.
            expectThat(oldDirectory.exists()).isFalse()

            expectThat(apolloWrapper.performedQueries).hasSize(2)
            expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()

            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY_WITH_FIRST_SPACE_DELETED)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY)

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            // Confirm SPACE_1 directory remains because the cleanup only runs once.
            expectThat(directory.exists()).isTrue()
            expectThat(apolloWrapper.performedQueries).hasSize(4)
            expectThat(apolloWrapper.performedQueries[2]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[3]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()
        }

    @Test
    fun refresh_schedulesAnExtraRefresh() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)
            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)

            spaceStore.refresh()
            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            expectThat(apolloWrapper.performedQueries).hasSize(3)
            expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedQueries[2]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()
        }

    @Test
    fun refresh_skipsWhileLoggedOut() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            neevaUser.neevaUserToken.setToken("")

            expectThat(neevaUser.neevaUserToken.getToken()).isEmpty()

            spaceStore.refresh()

            expectThat(apolloWrapper.performedQueries).isEmpty()
            expectThat(apolloWrapper.performedMutations).isEmpty()
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
            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)

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

            expectThat(apolloWrapper.performedQueries).hasSize(2)
            expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()
        }

    @Test
    fun refresh_withValidResponses_returnsAllData() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)

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
                        Uri.parse("https://developer.android.com/jetpack/compose/testing")
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
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            ).isTrue()

            expectThat(apolloWrapper.performedQueries).hasSize(2)
            expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()
        }

    @Test
    fun refresh_updatesWhenSpaceTimestampChanges() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)

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
                        Uri.parse("https://developer.android.com/jetpack/compose/testing")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            expectThat(apolloWrapper.performedQueries).hasSize(2)
            expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()

            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY)

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
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            ).isFalse()
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://reddit.com/r/android")
                )
            ).isTrue()

            expectThat(apolloWrapper.performedQueries).hasSize(4)
            expectThat(apolloWrapper.performedQueries[2]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[3]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()
        }

    @Test
    fun addToSpace_mutatesAndRefreshesWithGetSpacesOnly() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)

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
                        Uri.parse("https://developer.android.com/jetpack/compose/testing")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            expectThat(apolloWrapper.performedQueries).hasSize(2)
            expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()

            apolloWrapper.addResponse(RESPONSE_ADD_TO_SPACE_MUTATION)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY_URL_ADDED)

            val success = spaceStore.addOrRemoveFromSpace(
                SPACE_2.pageMetadata?.pageID!!,
                Uri.parse("https://example.com"),
                "Example page"
            )

            expectThat(success).isTrue()

            expectThat(apolloWrapper.performedMutations).hasSize(1)
            expectThat(apolloWrapper.performedMutations[0]).isA<AddToSpaceMutation>()

            expectThat(apolloWrapper.performedQueries).hasSize(3)
            expectThat(apolloWrapper.performedQueries[2]).isA<GetSpacesDataQuery>()

            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://example.com")
                )
            ).isTrue()
        }

    @Test
    fun deleteFromSpace_mutatesAndRefreshesWithGetSpacesOnly() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)

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
                            Uri.parse("https://developer.android.com/jetpack/compose/testing")
                        )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            expectThat(apolloWrapper.performedQueries).hasSize(2)
            expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
            expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
            expectThat(apolloWrapper.performedMutations).isEmpty()

            apolloWrapper.addResponse(RESPONSE_DELETE_FROM_SPACE_MUTATION)
            apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY_URL_DELETED)

            val success = spaceStore.addOrRemoveFromSpace(
                SPACE_2.pageMetadata?.pageID!!,
                database.spaceDao().getItemsFromSpace(SPACE_2.pageMetadata?.pageID!!).first().url!!,
                ""
            )

            expectThat(success).isTrue()

            expectThat(apolloWrapper.performedMutations).hasSize(1)
            expectThat(apolloWrapper.performedMutations[0]).isA<DeleteSpaceResultByURLMutation>()
            expectThat(apolloWrapper.performedQueries).hasSize(3)
            expectThat(apolloWrapper.performedQueries[2]).isA<GetSpacesDataQuery>()

            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            ).isFalse()
        }

    companion object {
        private val SPACE_1 = ListSpacesQuery.Space(
            pageMetadata = ListSpacesQuery.PageMetadata(pageID = "c5rgtmtdv9enb8j1gv60"),
            space = ListSpacesQuery.Space1(
                name = "Saved For Later",
                lastModifiedTs = "2022-02-10T22:08:01Z",
                userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Owner),
                acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
                hasPublicACL = true,
                resultCount = null,
                isDefaultSpace = true
            )
        )
        private val SPACE_2 = ListSpacesQuery.Space(
            pageMetadata = ListSpacesQuery.PageMetadata(
                pageID = "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
            ),
            space = ListSpacesQuery.Space1(
                name = "Jetpack Compose",
                lastModifiedTs = "2022-02-10T02:10:38Z",
                userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Comment),
                acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
                hasPublicACL = false,
                resultCount = 1,
                isDefaultSpace = false
            )
        )

        private val RESPONSE_LIST_SPACE_QUERY = """{
            "data": {
                "listSpaces": {
                    "requestID": "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
                    "space": [
                        {
                            "pageMetadata": {
                                "pageID": "c5rgtmtdv9enb8j1gv60"
                            },
                            "space": {
                                "name": "Saved For Later",
                                "lastModifiedTs": "2022-02-10T22:08:01Z",
                                "userACL": {
                                    "acl": "Owner"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "resultCount": null,
                                "isDefaultSpace": true
                            }
                        },
                        {
                            "pageMetadata": {
                                "pageID": "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space": {
                                "name": "Jetpack Compose",
                                "lastModifiedTs": "2022-02-10T02:10:38Z",
                                "userACL": {
                                    "acl": "Comment"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "resultCount": 1,
                                "isDefaultSpace": false
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED = """{
            "data": {
                "listSpaces": {
                    "requestID": "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc992",
                    "space": [
                        {
                            "pageMetadata": {
                                "pageID": "c5rgtmtdv9enb8j1gv60"
                            },
                            "space": {
                                "name": "Saved For Later",
                                "lastModifiedTs": "2022-02-10T22:08:01Z",
                                "userACL": {
                                    "acl": "Owner"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "resultCount": null,
                                "isDefaultSpace": true
                            }
                        },
                        {
                            "pageMetadata": {
                                "pageID": "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space": {
                                "name": "Jetpack Compose",
                                "lastModifiedTs": "2099-02-10T02:12:38Z",
                                "userACL": {
                                    "acl": "Comment"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "resultCount": 1,
                                "isDefaultSpace": false
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_LIST_SPACE_QUERY_WITH_FIRST_SPACE_DELETED = """{
            "data": {
                "listSpaces": {
                    "requestID": "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc992",
                    "space": [
                        {
                            "pageMetadata": {
                                "pageID": "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space": {
                                "name": "Jetpack Compose",
                                "lastModifiedTs": "2099-02-10T02:12:38Z",
                                "userACL": {
                                    "acl": "Comment"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "resultCount": 1,
                                "isDefaultSpace": false
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_GET_SPACES_DATA_QUERY = """{
            "data":{
                "getSpace":{
                    "space":[
                        {
                            "pageMetadata":{
                                "pageID":"c5rgtmtdv9enb8j1gv60"
                            },
                            "space":{
                                "thumbnail":"data:image/jpeg;base64,/9j/2wCEAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDIBCQkJDAsMGA0NGDIhHCEyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMv/AABEIAKgBQAMBIgACEQEDEQH/xAGiAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCgsQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+gEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoLEQACAQIEBAMEBwUEBAABAncAAQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4+fr/2gAMAwEAAhEDEQA/APf6KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo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",
                                "entities":[]
                            }
                        },
                        {
                            "pageMetadata":{
                                "pageID":"nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space":{
                                "thumbnail":"data:image/jpeg;base64,/9j/2wCEAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDIBCQkJDAsMGA0NGDIhHCEyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMv/AABEIAKgBQAMBIgACEQEDEQH/xAGiAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCgsQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+gEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoLEQACAQIEBAMEBwUEBAABAncAAQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4+fr/2gAMAwEAAhEDEQA/APf6KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo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",
                                "entities":[
                                    {
                                        "metadata":{
                                            "docID":"skadksnflkaamda345"
                                        },
                                        "spaceEntity":{
                                            "url":"https://developer.android.com/jetpack/compose/testing",
                                            "title":"Testing your Compose layout | Jetpack Compose | Android Developers",
                                            "snippet":null,
                                            "thumbnail":"data:image/jpeg;base64,garbage"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY = """{
            "data":{
                "getSpace":{
                    "space":[
                        {
                            "pageMetadata":{
                                "pageID":"nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space":{
                                "thumbnail":"data:image/jpeg;base64,/9j/2wCEAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDIBCQkJDAsMGA0NGDIhHCEyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMv/AABEIAKgBQAMBIgACEQEDEQH/xAGiAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCgsQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+gEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoLEQACAQIEBAMEBwUEBAABAncAAQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4+fr/2gAMAwEAAhEDEQA/APf6KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo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",
                                "entities":[
                                    {
                                        "metadata":{
                                            "docID":"skadksnflkaamda345"
                                        },
                                        "spaceEntity":{
                                            "url":"https://reddit.com/r/android",
                                            "title":"Android subreddit",
                                            "snippet":null,
                                            "thumbnail":"data:image/jpeg;base64,also garbage"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY_URL_ADDED = """{
            "data":{
                "getSpace":{
                    "space":[
                        {
                            "pageMetadata":{
                                "pageID":"nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space":{
                                "entities":[
                                    {
                                        "metadata":{
                                            "docID":"skadksnflkaamda345"
                                        },
                                        "spaceEntity":{
                                            "url":"https://developer.android.com/jetpack/compose/testing",
                                            "title":"Testing your Compose layout | Jetpack Compose | Android Developers",
                                            "snippet":null,
                                            "thumbnail":"data:image/jpeg;base64,garbage"
                                        }
                                    },
                                    {
                                        "metadata":{
                                            "docID":"sdknskdnskdn367"
                                        },
                                        "spaceEntity":{
                                            "url":"https://example.com",
                                            "title":"Example page",
                                            "snippet":null,
                                            "thumbnail":"data:image/jpeg;base64,also garbage"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY_URL_DELETED = """{
            "data":{
                "getSpace":{
                    "space":[
                        {
                            "pageMetadata":{
                                "pageID":"nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space":{
                                "entities":[]
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_ADD_TO_SPACE_MUTATION = """{
            "data":{
                "entityId":"nEgvD5HST7e62eEmfg0kkxx4xnEuNHBeEXxbGcoo"
            }
        }
        """.trimIndent()

        private val RESPONSE_DELETE_FROM_SPACE_MUTATION = """{
            "data":{
                "deleteSpaceResultByURL":true
            }
        }
        """.trimIndent()
    }
}
